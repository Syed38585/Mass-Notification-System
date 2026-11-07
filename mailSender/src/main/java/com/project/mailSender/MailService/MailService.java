package com.project.mailSender.MailService;

import com.project.mailSender.Model.MailLog;
import com.project.mailSender.Model.MailRecord;
import com.project.mailSender.Model.MailRecordDTO;
import com.project.mailSender.Model.MailRequest;
import com.project.mailSender.Model.Users;

import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Service.MailRecordService;
import com.project.mailSender.Service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

@Service
public class MailService {

    private static final ZoneId IST_ZONE =
            ZoneId.of("Asia/Kolkata");

    @Autowired
    private UserService userService;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private MailLogRepo mailLogRepo;

    @Autowired
    private MailRecordService mailRecordService;

    @Autowired
    private MailSenderClass mailSender;


    // =========================================================
    // MAIN DISPATCH METHOD
    // =========================================================

    public void processDispatch(
            MailRequest mailRequest,
            String gender,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        if (mailRequest == null) {
            throw new IllegalArgumentException(
                    "Mail request cannot be null"
            );
        }

        if (mailRequest.getSendAt() != null) {

            LocalDateTime nowIST =
                    ZonedDateTime.now(IST_ZONE)
                            .toLocalDateTime();

            if (!mailRequest.getSendAt().isAfter(nowIST)) {
                throw new IllegalArgumentException(
                        "Scheduled time must be in the future."
                );
            }

            scheduleDispatch(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

        } else {

            executeSend(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );
        }
    }


    // =========================================================
    // SCHEDULE EMAIL
    // =========================================================

    private void scheduleDispatch(
            MailRequest mailRequest,
            String gender,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        MailRecordDTO dto = new MailRecordDTO();

        dto.setSubject(mailRequest.getSubject());
        dto.setContent(mailRequest.getContent());

        int recipientCount;

        if (mailRequest.getRecipientIds() != null
                && !mailRequest.getRecipientIds().isEmpty()) {

            dto.setTargetGroup("selected-users");

            dto.setRecipientIds(
                    mailRequest.getRecipientIds().toString()
            );

            recipientCount =
                    sendToSelectedRecipientsCount(mailRequest);

        } else if (gender != null && !gender.isBlank()) {

            String normalizedGender =
                    normalizeGender(gender);

            dto.setTargetGroup(normalizedGender);

            recipientCount =
                    getSubscribedUsersByGender(normalizedGender).size();

        } else {

            dto.setTargetGroup("all");

            recipientCount =
                    getSubscribedUsers().size();
        }

        dto.setRecipientCount(recipientCount);
        dto.setRecipientNames(dto.getTargetGroup());

        dto.setHasAttachments(
                (imageBytes != null && imageBytes.length > 0)
                        || (pdfBytes != null && pdfBytes.length > 0)
        );

        dto.setStatus("SCHEDULED");
        dto.setScheduledAt(mailRequest.getSendAt());

        MailRecord savedRecord =
                mailRecordService.saveScheduledRecord(dto);

        Long recordId = savedRecord.getId();

        Date scheduleDate = Date.from(
                mailRequest.getSendAt()
                        .atZone(IST_ZONE)
                        .toInstant()
        );

        taskScheduler.schedule(() -> {

            System.out.println(
                    "Executing scheduled email: "
                            + mailRequest.getSubject()
            );

            try {

                executeSend(
                        mailRequest,
                        gender,
                        imageBytes,
                        imageName,
                        pdfBytes,
                        pdfName
                );

                mailRecordService.updateStatus(
                        recordId,
                        "SENT"
                );

                System.out.println(
                        "Scheduled email dispatched successfully."
                );

            } catch (Exception e) {

                mailRecordService.updateStatus(
                        recordId,
                        "FAILED"
                );

                System.err.println(
                        "Scheduled email execution failed: "
                                + e.getMessage()
                );

                e.printStackTrace();
            }

        }, scheduleDate);

        System.out.println(
                "Email scheduled for: " + mailRequest.getSendAt()
        );
    }


    // =========================================================
    // EXECUTE EMAIL SENDING
    // =========================================================

    private void executeSend(
            MailRequest mailRequest,
            String gender,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        Instant start = Instant.now();

        int totalRecipients;

        if (mailRequest.getRecipientIds() != null
                && !mailRequest.getRecipientIds().isEmpty()) {

            totalRecipients = sendToSelectedRecipients(
                    mailRequest,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

        } else if (gender == null || gender.isBlank()) {

            totalRecipients = sendToAll(
                    mailRequest,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

        } else {

            totalRecipients = sendByGender(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );
        }

        long duration =
                Duration.between(start, Instant.now()).toMillis();

        MailLog log = new MailLog();

        if (mailRequest.getRecipientIds() != null
                && !mailRequest.getRecipientIds().isEmpty()) {

            log.setTargetGroup("selected-users");

        } else if (gender != null && !gender.isBlank()) {

            log.setTargetGroup(normalizeGender(gender));

        } else {

            log.setTargetGroup("all");
        }

        log.setSentAt(LocalDateTime.now(IST_ZONE));
        log.setDurationMs(duration);
        log.setTotalRecipients(totalRecipients);

        mailLogRepo.save(log);

        System.out.println(
                "Total recipients queued: " + totalRecipients
        );

        System.out.println(
                "Total dispatch time: " + duration + " ms"
        );
    }


    // =========================================================
    // SEND TO ALL SUBSCRIBED USERS
    // =========================================================

    public int sendToAll(
            MailRequest mailRequest,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        List<Users> users = getSubscribedUsers();

        for (Users user : users) {

            mailSender.sendEmailAsync(
                    user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );
        }

        return users.size();
    }


    // =========================================================
    // SEND BY GENDER
    // =========================================================

    public int sendByGender(
            MailRequest mailRequest,
            String gender,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        String normalizedGender = normalizeGender(gender);

        List<Users> users =
                getSubscribedUsersByGender(normalizedGender);

        for (Users user : users) {

            mailSender.sendEmailAsync(
                    user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );
        }

        return users.size();
    }


    // =========================================================
    // SEND TO SELECTED RECIPIENTS
    // =========================================================

    public int sendToSelectedRecipients(
            MailRequest mailRequest,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        List<Long> ids = mailRequest.getRecipientIds();

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        List<Users> users =
                userService.getUsersByIds(ids)
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(Users::isSubscribed)
                        .toList();

        for (Users user : users) {

            mailSender.sendEmailAsync(
                    user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );
        }

        return users.size();
    }


    // =========================================================
    // HELPER: GET SUBSCRIBED USERS
    // =========================================================

    private List<Users> getSubscribedUsers() {

        return userService.getAll()
                .stream()
                .filter(Objects::nonNull)
                .filter(Users::isSubscribed)
                .toList();
    }


    // =========================================================
    // HELPER: GET SUBSCRIBED USERS BY GENDER
    // =========================================================

    private List<Users> getSubscribedUsersByGender(
            String gender) {

        return userService.sendByGender(gender)
                .stream()
                .filter(Objects::nonNull)
                .filter(Users::isSubscribed)
                .filter(user ->
                        user.getGender() != null
                                && gender.equalsIgnoreCase(
                                user.getGender().trim()
                        )
                )
                .toList();
    }


    // =========================================================
    // HELPER: NORMALIZE AND VALIDATE GENDER
    // =========================================================

    private String normalizeGender(String gender) {

        if (gender == null || gender.isBlank()) {
            throw new IllegalArgumentException(
                    "Gender cannot be empty."
            );
        }

        String normalized =
                gender.trim().toLowerCase(Locale.ROOT);

        if (!normalized.equals("male")
                && !normalized.equals("female")) {

            throw new IllegalArgumentException(
                    "Gender must be either 'male' or 'female'."
            );
        }

        return normalized;
    }


    // =========================================================
    // HELPER: COUNT SELECTED SUBSCRIBED USERS
    // =========================================================

    private int sendToSelectedRecipientsCount(
            MailRequest mailRequest) {

        List<Long> ids = mailRequest.getRecipientIds();

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        return userService.getUsersByIds(ids)
                .stream()
                .filter(Objects::nonNull)
                .filter(Users::isSubscribed)
                .toList()
                .size();
    }
}