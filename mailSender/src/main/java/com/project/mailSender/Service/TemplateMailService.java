package com.project.mailSender.Service;

import com.project.mailSender.Model.TemplateSendRequest;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Model.MailRecord;
import com.project.mailSender.Model.MailLog;

import com.project.mailSender.Repository.Repo;
import com.project.mailSender.Repository.MailLogRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TemplateMailService {

    private static final Logger log =
            LoggerFactory.getLogger(TemplateMailService.class);

    private static final ZoneId IST_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final Repo userRepo;
    private final MailLogRepo mailLogRepo;
    private final TemplateMailSender templateMailSender;
    private final MailRecordService mailRecordService;

    public TemplateMailService(
            Repo userRepo,
            MailLogRepo mailLogRepo,
            TemplateMailSender templateMailSender,
            MailRecordService mailRecordService) {

        this.userRepo = userRepo;
        this.mailLogRepo = mailLogRepo;
        this.templateMailSender = templateMailSender;
        this.mailRecordService = mailRecordService;
    }

    // =====================================================
    // MAIN TEMPLATE EMAIL METHOD
    // =====================================================

    @Transactional
    public int sendToSubscribedUsers(
            TemplateSendRequest request,
            MultipartFile[] files,
            String gender,
            List<Long> recipientIds) {

        long startTime = System.currentTimeMillis();

        // 1. VALIDATE REQUEST

        if (request == null) {
            throw new IllegalArgumentException(
                    "Email request cannot be null"
            );
        }

        if (request.getSubject() == null ||
                request.getSubject().isBlank()) {

            throw new IllegalArgumentException(
                    "Email subject cannot be empty"
            );
        }

        if (request.getHtmlBody() == null ||
                request.getHtmlBody().isBlank()) {

            throw new IllegalArgumentException(
                    "Email content cannot be empty"
            );
        }

        // 2. RESOLVE RECIPIENT IDS

        if (recipientIds == null || recipientIds.isEmpty()) {
            recipientIds = request.getRecipientIds();
        }

        if (recipientIds == null) {
            recipientIds = Collections.emptyList();
        }

        Set<Long> selectedIds = recipientIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. DETERMINE TARGET GROUP

        String targetGroup;

        if (!selectedIds.isEmpty()) {

            targetGroup = "selected-users";

        } else if (gender != null && !gender.isBlank()) {

            targetGroup = gender.trim().toLowerCase(Locale.ROOT);

        } else if (request.getTargetGroup() != null &&
                !request.getTargetGroup().isBlank()) {

            targetGroup = request.getTargetGroup()
                    .trim()
                    .toLowerCase(Locale.ROOT);

        } else {

            targetGroup = "all";
        }

        // 4. VALIDATE TARGET GROUP

        Set<String> allowedGroups = Set.of(
                "all",
                "male",
                "female",
                "selected-users"
        );

        if (!allowedGroups.contains(targetGroup)) {
            throw new IllegalArgumentException(
                    "Invalid target group: " + targetGroup
            );
        }

        if (targetGroup.equals("selected-users") &&
                selectedIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select at least one recipient"
            );
        }

        final String finalTargetGroup = targetGroup;

        log.info("Selected audience: {}", finalTargetGroup);

        // 5. PROCESS ATTACHMENTS

        List<TemplateMailSender.AttachmentData> attachments =
                new ArrayList<>();

        if (files != null) {

            for (MultipartFile file : files) {

                if (file == null || file.isEmpty()) {
                    continue;
                }

                try {

                    validateAttachment(file);

                    attachments.add(
                            TemplateMailSender.AttachmentData.from(file)
                    );

                    log.info(
                            "Attachment added: {}",
                            file.getOriginalFilename()
                    );

                } catch (IOException e) {

                    throw new IllegalArgumentException(
                            "Unable to read attachment: "
                                    + file.getOriginalFilename(),
                            e
                    );
                }
            }
        }

        // 6. FETCH ALL USERS

        List<Users> allUsers = userRepo.findAll();

        log.info("Total users found: {}", allUsers.size());

        // 7. FILTER RECIPIENTS

        List<Users> recipients = allUsers.stream()

                .filter(Objects::nonNull)

                // Only subscribed users
                .filter(Users::isSubscribed)

                // Valid email addresses only
                .filter(user ->
                        user.getEmail() != null &&
                                !user.getEmail().isBlank()
                )

                // Audience filtering
                .filter(user -> {

                    switch (finalTargetGroup) {

                        case "male":
                            return user.getGender() != null &&
                                    user.getGender().trim()
                                            .equalsIgnoreCase("male");

                        case "female":
                            return user.getGender() != null &&
                                    user.getGender().trim()
                                            .equalsIgnoreCase("female");

                        case "selected-users":
                            return selectedIds.contains(user.getId());

                        case "all":
                            return true;

                        default:
                            return false;
                    }
                })

                .collect(Collectors.toList());

        log.info(
                "Final recipients for {}: {}",
                finalTargetGroup,
                recipients.size()
        );

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "No subscribed recipients found for: "
                            + finalTargetGroup
            );
        }

        // 8. SEND EMAILS

        int queued = 0;

        for (Users user : recipients) {

            try {

                templateMailSender.sendOne(
                        request.getSubject(),
                        request.getHtmlBody(),
                        user,
                        attachments
                );

                queued++;

            } catch (Exception e) {

                log.error(
                        "Could not queue email for user ID: {}",
                        user.getId(),
                        e
                );
            }
        }

        // 9. CALCULATE SEND DURATION

        long durationMs =
                System.currentTimeMillis() - startTime;

        // =================================================
        // 10. SAVE MAIL LOG FOR ANALYTICS
        // =================================================

        if (queued > 0) {

            MailLog mailLog = new MailLog();

            mailLog.setTargetGroup(finalTargetGroup);

            mailLog.setSentAt(
                    LocalDateTime.now(IST_ZONE)
            );

            mailLog.setDurationMs(durationMs);

            mailLog.setTotalRecipients(queued);

            mailLogRepo.save(mailLog);

            log.info(
                    "Analytics MailLog saved. Target: {}, Recipients: {}, Duration: {} ms",
                    finalTargetGroup,
                    queued,
                    durationMs
            );
        }

        // =================================================
        // 11. SAVE CAMPAIGN HISTORY
        // =================================================

        if (queued > 0) {

            String audienceLabel;

            switch (finalTargetGroup) {

                case "male":
                    audienceLabel = "MALE USERS";
                    break;

                case "female":
                    audienceLabel = "FEMALE USERS";
                    break;

                case "selected-users":
                    audienceLabel = "SELECTED USERS";
                    break;

                default:
                    audienceLabel = "ALL SUBSCRIBED USERS";
            }

            MailRecord savedRecord =
                    mailRecordService.saveSentRecord(
                            request.getSubject(),
                            request.getHtmlBody(),
                            audienceLabel,
                            null,
                            null,
                            queued,
                            !attachments.isEmpty(),
                            durationMs
                    );

            log.info(
                    "MailRecord saved. ID: {}",
                    savedRecord.getId()
            );
        }

        log.info(
                "Template campaign completed. Group: {}, Queued: {}",
                finalTargetGroup,
                queued
        );

        return queued;
    }

    // =====================================================
    // ATTACHMENT VALIDATION
    // =====================================================

    private void validateAttachment(MultipartFile file) {

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Attachment filename is missing"
            );
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);

        boolean allowed =
                lowerName.endsWith(".pdf") ||
                        lowerName.endsWith(".jpg") ||
                        lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") ||
                        lowerName.endsWith(".gif") ||
                        lowerName.endsWith(".webp");

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Unsupported attachment type: " + filename
            );
        }

        // Maximum 10 MB per attachment

        long maxSize = 10L * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "Attachment exceeds 10 MB: " + filename
            );
        }
    }
}