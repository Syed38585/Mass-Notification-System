package com.project.mailSender.MailService;

import com.project.mailSender.Model.MailLog;
import com.project.mailSender.Model.MailRequest;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class MailService {

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private MailLogRepo mailLogRepo;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String sender;

    // ==========================================
    // MAIN ENTRY POINT
    // ==========================================
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

    // ==========================================
    // SCHEDULER
    // ==========================================
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

    // ==========================================
    // EXECUTION
    // ==========================================
    private void executeSend(MailRequest mailRequest,
                             String gender,
                             byte[] imageBytes,
                             String imageName,
                             byte[] pdfBytes,
                             String pdfName) {

        Instant start = Instant.now();

        int totalRecipients;

        if (gender == null || gender.isEmpty()) {

            totalRecipients = sendToAll(mailRequest,
                    imageBytes, imageName,
                    pdfBytes, pdfName);

        } else {

            totalRecipients = sendByGender(mailRequest, gender,
                    imageBytes, imageName,
                    pdfBytes, pdfName);
        }

        Instant end = Instant.now();
        long duration = Duration.between(start, end).toMillis();

        MailLog log = new MailLog();
        log.setTargetGroup(gender == null ? "all" : gender);
        log.setSentAt(LocalDateTime.now());
        log.setDurationMs(duration);
        log.setTotalRecipients(totalRecipients);

        mailLogRepo.save(log);

        System.out.println("⏱ Total Send Time: " + duration + " ms");
    }

    // ==========================================
    // SEND TO ALL
    // ==========================================
    public int sendToAll(MailRequest mailRequest,
                         byte[] imageBytes,
                         String imageName,
                         byte[] pdfBytes,
                         String pdfName) {

        List<Users> users = userService.getAll();

        for (Users user : users) {

            sendEmailAsync(user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName);
        }

        return users.size();
    }

    // ==========================================
    // SEND BY GENDER
    // ==========================================
    public int sendByGender(MailRequest mailRequest,
                            String gender,
                            byte[] imageBytes,
                            String imageName,
                            byte[] pdfBytes,
                            String pdfName) {

        List<Users> users = userService.sendByGender(gender);

        for (Users user : users) {

            sendEmailAsync(user,
                    mailRequest.getSubject(),
                    mailRequest.getContent(),
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName);
        }

        return users.size();
    }

    // ==========================================
    // ASYNC EMAIL SENDING
    // ==========================================
    @Async("mailExecutor")
    public void sendEmailAsync(Users user,
                               String subject,
                               String content,
                               byte[] imageBytes,
                               String imageName,
                               byte[] pdfBytes,
                               String pdfName) {

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            helper.setFrom(sender);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);

            // ✅ Encode email (CRITICAL FIX)
            String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

            // ✅ Tracking image (visible logo style)
            String logoTracker = "<div style='text-align:center;margin-bottom:20px;'>"
                    + "<img src='" + baseUrl + "/track?email="
                    + encodedEmail
                    + "&emailId=" + UUID.randomUUID()
                    + "' width='200' height='80' />"
                    + "</div>";

            // ✅ Final HTML content
            String finalContent = "<html><body>"
                    + logoTracker
                    + "<div>" + content + "</div>"
                    + "</body></html>";

            helper.setText(finalContent, true);

            // Attach image
            if (imageBytes != null) {
                helper.addAttachment(
                        imageName,
                        new ByteArrayResource(imageBytes)
                );
            }

            // Attach PDF
            if (pdfBytes != null) {
                helper.addAttachment(
                        pdfName,
                        new ByteArrayResource(pdfBytes)
                );
            }

            mailSender.send(message);

            System.out.println("✅ Sent to: " + user.getEmail());

        } catch (Exception e) {

            System.err.println("❌ Failed for: " + user.getEmail()
                    + " | Reason: " + e.getMessage());
        }
    }
}