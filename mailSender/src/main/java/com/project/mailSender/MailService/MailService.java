package com.project.mailSender.MailService;

import com.project.mailSender.Model.MailLog;
import com.project.mailSender.Model.MailRequest;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Service.UnsubscribeTokenService;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.Date;
import java.util.List;

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

    @Autowired
    UnsubscribeTokenService unsubscribeTokenService;

//    @Value("${app.base-url}")
//    private String baseUrl;

    @Value("${spring.mail.username}")
    private String sender;



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

    // =========================================================
    // SEND EMAIL
    // =========================================================

    @Async("mailExecutor")
    public void sendEmailAsync(
            Users user,
            String subject,
            String content,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        try {

            // =================================================
            // 1. CHECK IF USER IS SUBSCRIBED
            // =================================================

            if (!user.isSubscribed()) {

                System.out.println(
                        "Skipping unsubscribed user: "
                                + user.getEmail()
                );

                return;
            }


            // =================================================
            // 2. GENERATE UNSUBSCRIBE TOKEN
            // =================================================

            String token =
                    unsubscribeTokenService.generateToken(
                            user.getId()
                    );


            String unsubscribeUrl =
                    "http://172.20.10.2:8080"
                            + "/api/user/unsubscribe?token="
                            + token;


            // =================================================
            // 3. HANDLE NULL CONTENT
            // =================================================

            if (content == null) {
                content = "";
            }


            // =================================================
            // 4. ESCAPE HTML
            // =================================================

            String formattedContent = content
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("\r\n", "<br>")
                    .replace("\n", "<br>");


            // =================================================
            // 5. CALCULATE TEXT SIZE FROM IMAGE SIZE
            // =================================================

            int textSize = 17;

            if (imageBytes != null
                    && imageBytes.length > 0) {

                try {

                    BufferedImage image =
                            ImageIO.read(
                                    new ByteArrayInputStream(
                                            imageBytes
                                    )
                            );

                    if (image != null) {

                        int width =
                                image.getWidth();

                        int height =
                                image.getHeight();


                        int largestDimension =
                                Math.max(
                                        width,
                                        height
                                );


                        /*
                         * Text size based on image
                         *
                         * < 1200px     -> 16px
                         * 1200-1799px  -> 18px
                         * 1800-2499px  -> 21px
                         * 2500px+      -> 24px
                         */

                        if (largestDimension >= 2500) {

                            textSize = 28;

                        } else if (largestDimension >= 1800) {

                            textSize = 23;

                        } else if (largestDimension >= 1200) {

                            textSize = 20;

                        } else {

                            textSize = 16;
                        }
                    }

                } catch (Exception e) {

                    // If image size cannot be read,
                    // use the default size.

                    textSize = 17;
                }
            }


            // =================================================
            // 6. IMAGE SECTION
            // =================================================

            String imageSection = "";

            if (imageBytes != null
                    && imageBytes.length > 0) {

                imageSection = """

                        <div style="
                            margin-top:30px;
                            width:100%%;
                            text-align:center;
                        ">

                            <img
                                src="cid:mailImage"
                                alt="Attached Image"
                                style="
                                    display:block;
                                    width:100%%;
                                    max-width:100%%;
                                    height:auto;
                                    border-radius:12px;
                                    margin:0 auto;
                                "
                            >

                        </div>

                        """;
            }


            // =================================================
            // 7. PDF SECTION
            // =================================================

            String pdfSection = "";

            if (pdfBytes != null
                    && pdfBytes.length > 0) {

                String displayName =
                        pdfName != null
                                ? pdfName
                                : "document.pdf";


                pdfSection = """

                        <div style="
                            margin-top:30px;
                            padding:18px;
                            background:#f8fafc;
                            border:1px solid #e2e8f0;
                            border-radius:12px;
                            text-align:center;
                        ">

                            <div style="
                                font-size:14px;
                                font-weight:bold;
                                color:#334155;
                                margin-bottom:8px;
                            ">

                                📄 PDF Attachment

                            </div>


                            <div style="
                                font-size:12px;
                                color:#64748b;
                                word-break:break-word;
                            ">

                                %s

                            </div>


                            <div style="
                                margin-top:12px;
                                font-size:12px;
                                color:#2563eb;
                                font-weight:bold;
                            ">

                                📎 PDF attached to this email

                            </div>

                        </div>

                        """.formatted(
                        displayName
                );
            }


            // =================================================
            // 8. COMPLETE EMAIL HTML
            // =================================================

            String html = """

                    <!DOCTYPE html>

                    <html>

                    <head>

                        <meta charset="UTF-8">

                        <meta name="viewport"
                              content="width=device-width,
                                       initial-scale=1.0">

                    </head>


                    <body style="
                        margin:0;
                        padding:0;
                        background-color:#f1f5f9;
                        font-family:Arial,
                                     Helvetica,
                                     sans-serif;
                    ">


                    <!-- OUTER CONTAINER -->

                    <table
                        width="100%%"
                        cellpadding="0"
                        cellspacing="0"
                        border="0"
                        style="
                            background-color:#f1f5f9;
                            padding:40px 15px;
                        "
                    >

                        <tr>

                            <td align="center">


                                <!-- MAIN EMAIL BOX -->

                                <table
                                    width="600"
                                    cellpadding="0"
                                    cellspacing="0"
                                    border="0"
                                    style="
                                        max-width:600px;
                                        width:100%%;
                                        background:#ffffff;
                                        border-radius:16px;
                                        border:1px solid #e2e8f0;
                                        overflow:hidden;
                                    "
                                >


                                    <!-- =====================
                                         HEADER
                                    ====================== -->

                                    <tr>

                                        <td style="
                                            padding:24px 30px;
                                            background:#2563eb;
                                            color:#ffffff;
                                        ">

                                            <div style="
                                                font-size:22px;
                                                font-weight:bold;
                                            ">

                                                MailGen AI

                                            </div>

                                        </td>

                                    </tr>


                                    <!-- =====================
                                         MESSAGE + IMAGE + PDF
                                    ====================== -->

                                    <tr>

                                        <td style="
                                            padding:35px 30px;
                                            color:#1e293b;
                                            font-size:%spx;
                                            line-height:1.8;
                                            word-wrap:break-word;
                                            overflow-wrap:break-word;
                                        ">


                                            <!-- MESSAGE -->

                                            <div style="
                                                width:100%%;
                                                font-size:%spx;
                                                line-height:1.8;
                                            ">

                                                %s

                                            </div>


                                            <!-- IMAGE -->

                                            %s


                                            <!-- PDF -->

                                            %s


                                        </td>

                                    </tr>


                                    <!-- =====================
                                         DIVIDER
                                    ====================== -->

                                    <tr>

                                        <td style="
                                            padding:0 30px;
                                        ">

                                            <div style="
                                                height:1px;
                                                background:#e2e8f0;
                                            "></div>

                                        </td>

                                    </tr>


                                    <!-- =====================
                                         UNSUBSCRIBE
                                    ====================== -->

                                    <tr>

                                        <td
                                            align="center"
                                            style="
                                                padding:28px 30px 30px;
                                            "
                                        >

                                            <p style="
                                                margin:0 0 15px;
                                                font-size:12px;
                                                color:#94a3b8;
                                            ">

                                                Don't want to receive
                                                these emails?

                                            </p>


                                            <a
                                                href="%s"
                                                style="
                                                    display:inline-block;
                                                    padding:11px 24px;
                                                    background:#ffffff;
                                                    color:#dc2626;
                                                    border:1px solid #dc2626;
                                                    border-radius:8px;
                                                    text-decoration:none;
                                                    font-size:13px;
                                                    font-weight:bold;
                                                "
                                            >

                                                Unsubscribe

                                            </a>

                                        </td>

                                    </tr>


                                </table>


                                <!-- =====================
                                     FOOTER
                                ====================== -->

                                <p style="
                                    margin:18px 0 0;
                                    font-size:11px;
                                    color:#94a3b8;
                                ">

                                    © 2026 MailGen AI

                                </p>


                            </td>

                        </tr>

                    </table>


                    </body>

                    </html>

                    """.formatted(
                    textSize,
                    textSize,
                    formattedContent,
                    imageSection,
                    pdfSection,
                    unsubscribeUrl
            );


            // =================================================
            // 9. CREATE MIME MESSAGE
            // =================================================

            MimeMessage message =
                    mailSender.createMimeMessage();


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );


            // =================================================
            // 10. SET EMAIL INFORMATION
            // =================================================

            helper.setFrom(sender);

            helper.setTo(user.getEmail());

            helper.setSubject(subject);

            helper.setText(
                    html,
                    true
            );


            // =================================================
            // 11. ADD IMAGE INSIDE EMAIL
            // =================================================

            if (imageBytes != null
                    && imageBytes.length > 0) {

                String imageContentType =
                        getImageContentType(
                                imageName
                        );


                helper.addInline(
                        "mailImage",

                        new ByteArrayResource(
                                imageBytes
                        ),

                        imageContentType
                );
            }


            // =================================================
            // 12. ADD PDF AS REAL EMAIL ATTACHMENT
            // =================================================

            if (pdfBytes != null
                    && pdfBytes.length > 0) {

                helper.addAttachment(

                        pdfName != null
                                ? pdfName
                                : "document.pdf",

                        new ByteArrayResource(
                                pdfBytes
                        )
                );
            }


            // =================================================
            // 13. SEND EMAIL
            // =================================================

            mailSender.send(message);


            System.out.println(
                    "Email sent successfully to "
                            + user.getEmail()
            );


        } catch (Exception e) {

            System.err.println(
                    "Failed to send email to "
                            + user.getEmail()
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // GET IMAGE CONTENT TYPE
    // =========================================================

    private String getImageContentType(
            String imageName) {

        if (imageName == null) {
            return "image/jpeg";
        }


        String name =
                imageName.toLowerCase();


        if (name.endsWith(".png")) {

            return "image/png";
        }


        if (name.endsWith(".gif")) {

            return "image/gif";
        }


        if (name.endsWith(".webp")) {

            return "image/webp";
        }


        if (name.endsWith(".bmp")) {

            return "image/bmp";
        }
        return "image/jpeg";
    }

    public int sendToSelectedRecipients(
            MailRequest mailRequest,
            byte[] imageBytes,
            String imageName,
            byte[] pdfBytes,
            String pdfName) {

        List<Long> ids = mailRequest.getRecipientIds();

        List<Users> users = userService.getUsersByIds(ids);

        for (Users user : users) {

            sendEmailAsync(
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