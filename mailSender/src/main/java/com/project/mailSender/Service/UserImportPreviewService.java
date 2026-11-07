package com.project.mailSender.Service;


import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class UserImportPreviewService {

    private final Repo userRepository;


    public UserImportPreviewService(Repo userRepository) {
        this.userRepository = userRepository;
    }

    public PreviewResponse preview(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload an Excel file.");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported.");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("File size cannot exceed 2 MB.");
        }

        try (InputStream input = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(input)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);

            if (header == null) {
                throw new IllegalArgumentException("Excel header is missing.");
            }

            DataFormatter formatter = new DataFormatter();

            Map<String, Integer> columns = new HashMap<>();

            for (Cell cell : header) {
                String column = formatter.formatCellValue(cell)
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");

                columns.put(column, cell.getColumnIndex());
            }

            requireColumn(columns, "name");
            requireColumn(columns, "email");
            requireColumn(columns, "gender");
            requireColumn(columns, "dob");

            List<PreviewRow> rows = new ArrayList<>();

            Set<String> uploadedEmails = new HashSet<>();

            int ready = 0;
            int exists = 0;
            int invalid = 0;
            int duplicate = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) continue;

                String name = getValue(row, columns, "name", formatter);
                String email = getValue(row, columns, "email", formatter);
                String gender = getValue(row, columns, "gender", formatter);
                String rawDob = getValue(row, columns, "dob", formatter);
                String country = getValue(row, columns, "country", formatter);

                if (name.isBlank() && email.isBlank()
                        && gender.isBlank() && rawDob.isBlank()) {
                    continue;
                }

                String status;
                String note = "";

                String normalizedEmail = email.toLowerCase(Locale.ROOT);

                LocalDate dob = parseDob(row, columns.get("dob"), formatter);

                if (name.isBlank() || email.isBlank()
                        || gender.isBlank() || rawDob.isBlank()) {

                    status = "INVALID";
                    note = "Required fields are missing.";

                } else if (!email.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

                    status = "INVALID";
                    note = "Invalid email address.";

                } else if (dob == null || dob.isAfter(LocalDate.now())) {

                    status = "INVALID";
                    note = "Invalid date of birth.";

                } else if (!uploadedEmails.add(normalizedEmail)) {

                    status = "DUPLICATE";
                    note = "Duplicate email in uploaded file.";

                } else if (userRepository
                        .existsByEmailIgnoreCase(email)) {

                    status = "EXISTS";
                    note = "User already exists.";

                } else {

                    status = "READY";
                    note = "Ready to import.";
                }

                switch (status) {
                    case "READY" -> ready++;
                    case "EXISTS" -> exists++;
                    case "INVALID" -> invalid++;
                    case "DUPLICATE" -> duplicate++;
                }

                Integer age = dob == null ? null : calculateAge(dob);

                rows.add(new PreviewRow(
                        i + 1,
                        name,
                        email,
                        gender,
                        dob == null ? null : dob.toString(),
                        status,
                        note,
                        country,
                        age
                ));
            }

            return new PreviewResponse(
                    rows.size(),
                    ready,
                    exists,
                    0,
                    invalid,
                    duplicate,
                    rows
            );

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to read Excel file: " + e.getMessage()
            );
        }
    }

    private void requireColumn(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Missing required Excel column: " + name
            );
        }
    }

    private String getValue(
            Row row,
            Map<String, Integer> columns,
            String column,
            DataFormatter formatter) {

        Integer index = columns.get(column);

        if (index == null) return "";

        Cell cell = row.getCell(index);

        if (cell == null) return "";

        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate parseDob(
            Row row,
            Integer columnIndex,
            DataFormatter formatter) {

        if (columnIndex == null) return null;

        Cell cell = row.getCell(columnIndex);

        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {

                return DateUtil.getLocalDateTime(
                        cell.getNumericCellValue()
                ).toLocalDate();
            }

            String value = formatter.formatCellValue(cell).trim();

            if (value.isBlank()) return null;

            List<DateTimeFormatter> formats = List.of(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("d/M/yyyy")
            );

            for (DateTimeFormatter format : formats) {
                try {
                    return LocalDate.parse(value, format);
                } catch (DateTimeParseException ignored) {
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private int calculateAge(LocalDate dob) {

        return java.time.Period.between(
                dob,
                LocalDate.now()
        ).getYears();
    }

    @Transactional
    public CommitResponse commit(
            MultipartFile file,
            boolean consent) {

        if (!consent) {
            throw new IllegalArgumentException(
                    "Please provide consent before importing users."
            );
        }

        // Validate Excel and obtain preview data
        UserImportPreviewService.PreviewResponse preview =
                preview(file);

        List<Users> usersToSave = new ArrayList<>();

        List<SkippedRow> skippedRows = new ArrayList<>();

        for (UserImportPreviewService.PreviewRow row : preview.rows()) {

            // Skip rows that failed preview validation
            if (!"READY".equals(row.status())) {

                skippedRows.add(
                        new SkippedRow(
                                row.row(),
                                row.name(),
                                row.email(),
                                row.status(),
                                row.note()
                        )
                );

                continue;
            }

            // Check again to prevent duplicate insertion
            if (userRepository.existsByEmailIgnoreCase(row.email())) {

                skippedRows.add(
                        new SkippedRow(
                                row.row(),
                                row.name(),
                                row.email(),
                                "EXISTS",
                                "User already exists."
                        )
                );

                continue;
            }

            // Create user entity
            Users user = new Users();

            user.setName(row.name());
            user.setEmail(row.email());
            user.setGender(row.gender());
            user.setCountry(row.country());
            user.setDob(row.dob());
            user.setAge(row.age());

            usersToSave.add(user);
        }

        // Save all valid users
        List<Users> savedUsers = userRepository.saveAll(usersToSave);

        int added = savedUsers.size();
        int skipped = skippedRows.size();

        return new CommitResponse(
                added,
                skipped,
                skippedRows
        );
    }

    public record CommitResponse(
            int added,
            int skipped,
            List<SkippedRow> skippedRows
    ) {}

    public record SkippedRow(
            int row,
            String name,
            String email,
            String status,
            String note
    ) {}

    public record PreviewResponse(
            int total,
            int ready,
            int exists,
            int trash,
            int invalid,
            int duplicate,
            List<PreviewRow> rows
    ) {}

    public record PreviewRow(
            int row,
            String name,
            String email,
            String gender,
            String dob,
            String status,
            String note,
            String country,
            Integer age
    ) {}
}