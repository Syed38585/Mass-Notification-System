package com.project.mailSender.MailService;

import com.project.mailSender.Model.MailLog;
import com.project.mailSender.Model.MailRequest;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Service.JwtService;
import com.project.mailSender.Service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;
import java.util.List;

@Service
public class MailService {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private MailLogRepo mailLogRepo;

    @Autowired
    private JwtService jwtService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String sender;

    @Autowired
    MailSenderClass mailSender;


//    @Value("${app.base-url}")
//    String appBaseUrl;

    public void processDispatch(MailRequest mailRequest,
                                String gender,
                                byte[] imageBytes,
                                String imageName,
                                byte[] pdfBytes,
                                String pdfName) {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        if (mailRequest.getSendAt() != null) {

            LocalDateTime nowIST = ZonedDateTime.now(istZone).toLocalDateTime();

            if (mailRequest.getSendAt().isBefore(nowIST)) {
                throw new IllegalArgumentException("Scheduled time must be in the future.");
            }

            scheduleDispatch(mailRequest, gender,
                    imageBytes, imageName,
                    pdfBytes, pdfName);

        } else {

            executeSend(mailRequest, gender,
                    imageBytes, imageName,
                    pdfBytes, pdfName);
        }
    }

    private void scheduleDispatch(MailRequest mailRequest,
                                  String gender,
                                  byte[] imageBytes,
                                  String imageName,
                                  byte[] pdfBytes,
                                  String pdfName) {

        Date scheduleDate = Date.from(
                mailRequest.getSendAt()
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toInstant()
        );

        taskScheduler.schedule(() -> {

            System.out.println("🔔 Executing Scheduled Mail...");

            executeSend(mailRequest, gender,
                    imageBytes, imageName,
                    pdfBytes, pdfName);

        }, scheduleDate);
    }

    private void executeSend(MailRequest mailRequest,
                             String gender,
                             byte[] imageBytes,
                             String imageName,
                             byte[] pdfBytes,
                             String pdfName) {

        Instant start = Instant.now();

        int totalRecipients;
        if (mailRequest.getRecipientIds() != null &&
                !mailRequest.getRecipientIds().isEmpty()) {

            totalRecipients = sendToSelectedRecipients(
                    mailRequest,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

        } else if (gender == null || gender.isEmpty()) {

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

        Instant end = Instant.now();
        long duration = Duration.between(start, end).toMillis();

        MailLog log = new MailLog();
        if (mailRequest.getRecipientIds() != null &&
                !mailRequest.getRecipientIds().isEmpty()) {

            log.setTargetGroup("selected-users");

        } else if (gender != null && !gender.isBlank()) {

            log.setTargetGroup(gender);

        } else {

            log.setTargetGroup("all");
        }        log.setSentAt(LocalDateTime.now());
        log.setDurationMs(duration);
        log.setTotalRecipients(totalRecipients);

        mailLogRepo.save(log);

        System.out.println("⏱ Total Send Time: " + duration + " ms");
    }

    public int sendToAll(MailRequest mailRequest,
                         byte[] imageBytes,
                         String imageName,
                         byte[] pdfBytes,
                         String pdfName) {

        List<Users> users = userService.getAll();

        for (Users user : users) {

            mailSender.sendEmailAsync(user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName);
        }

        return users.size();
    }

    public int sendByGender(MailRequest mailRequest,
                            String gender,
                            byte[] imageBytes,
                            String imageName,
                            byte[] pdfBytes,
                            String pdfName) {

        List<Users> users = userService.sendByGender(gender);

        for (Users user : users) {

            mailSender.sendEmailAsync(user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName);
        }

        return users.size();
    }

    // =========================================================
    // SEND EMAIL
    // =========================================================




    // =========================================================
    // GET IMAGE CONTENT TYPE
    // =========================================================


    public int sendToSelectedRecipients(
            MailRequest mailRequest,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        List<Long> ids = mailRequest.getRecipientIds();

        List<Users> users = userService.getUsersByIds(ids);

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
}
