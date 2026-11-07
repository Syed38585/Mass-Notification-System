package com.project.mailSender.Service;

import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvUserImportService {
    private final Repo repo;
    public CsvUserImportService(Repo repo) { this.repo = repo; }

    public Map<String, Object> importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Upload a non-empty CSV file.");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv") && !filename.endsWith(".txt")) {
            throw new IllegalArgumentException("Only CSV or comma-delimited TXT files are supported.");
        }

        int inserted = 0, skipped = 0, invalid = 0, rowNumber = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            Map<String, Integer> columns = null;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;
                List<String> cells = parseCsvLine(line);
                if (columns == null) {
                    columns = new HashMap<>();
                    for (int i = 0; i < cells.size(); i++)
                        columns.put(cells.get(i).trim().toLowerCase(Locale.ROOT), i);
                    if (!columns.containsKey("email") || !columns.containsKey("name"))
                        throw new IllegalArgumentException("CSV header must contain at least name,email. Optional: gender,country,age,dob.");
                    continue;
                }

                try {
                    String name = value(cells, columns, "name");
                    String email = value(cells, columns, "email").toLowerCase(Locale.ROOT);
                    if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                        throw new IllegalArgumentException("name or email is invalid");
                    if (!seen.add(email) || repo.findByEmail(email) != null) { skipped++; continue; }

                    Users user = new Users();
                    user.setName(name.trim());
                    user.setEmail(email);
                    user.setGender(value(cells, columns, "gender"));
                    user.setCountry(value(cells, columns, "country"));
                    user.setDob(value(cells, columns, "dob"));
                    String age = value(cells, columns, "age");
                    user.setAge(age.isBlank() ? 0 : Integer.parseInt(age));
                    user.setSubscribed(true);
                    repo.save(user);
                    inserted++;
                } catch (Exception rowError) {
                    invalid++;
                    errors.add("Row " + rowNumber + ": " + rowError.getMessage());
                }
            }
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException) throw (IllegalArgumentException) ex;
            throw new IllegalArgumentException("Unable to read CSV: " + ex.getMessage(), ex);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("skippedDuplicates", skipped);
        result.put("invalidRows", invalid);
        result.put("errors", errors);
        return result;
    }

    private String value(List<String> cells, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        return index == null || index >= cells.size() ? "" : cells.get(index).trim();
    }

    // Handles quoted CSV cells and escaped quotes; one physical line per record.
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(cell.toString()); cell.setLength(0);
            } else cell.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted CSV value.");
        result.add(cell.toString());
        return result;
    }
}
