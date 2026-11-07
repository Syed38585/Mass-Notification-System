package com.project.mailSender.Service;

import com.project.mailSender.Model.Users;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateMailSender {

    private final JavaMailSender mailSender;
    private final JwtService jwtService;

    private final String sender;
    private final String baseUrl;

    public TemplateMailSender(
            JavaMailSender mailSender,
            JwtService jwtService,
            @Value("${spring.mail.username}") String sender,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {

        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.sender = sender;
        this.baseUrl = baseUrl;
    }

    // =====================================================
    // ATTACHMENT DATA
    // =====================================================

    public record AttachmentData(
            String filename,
            byte[] content,
            String contentType
    ) {

        public static AttachmentData from(MultipartFile file)
                throws IOException {

            return new AttachmentData(
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType()
            );
        }
    }

    // =====================================================
    // SEND EMAIL
    // =====================================================

    @Async("mailExecutor")
    public void sendOne(
            String subject,
            String htmlBody,
            Users user,
            List<AttachmentData> attachments) {

        try {

            // 1. VALIDATE USER

            if (user == null ||
                    user.getEmail() == null ||
                    user.getEmail().isBlank() ||
                    !user.isSubscribed()) {

                return;
            }

            // 2. PERSONALIZE EMAIL

            Integer age = user.getAge();

            String body = htmlBody == null ? "" : htmlBody;

            body = body
                    .replace("{{name}}", html(user.getName()))
                    .replace("{{email}}", html(user.getEmail()))
                    .replace("{{country}}", html(user.getCountry()))
                    .replace("{{gender}}", html(user.getGender()))
                    .replace(
                            "{{age}}",
                            age == null ? "" : String.valueOf(age)
                    );

            // 3. GENERATE UNSUBSCRIBE TOKEN

            String token =
                    jwtService.generateUnsubscribeToken(user.getId());

            String unsubscribeUrl =
                    baseUrl + "/api/user/unsubscribe?token=" + token;

            // 4. EMAIL HTML TEMPLATE

            String htmlContent = """

                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport"
                              content="width=device-width, initial-scale=1.0">
                        <title>Mailflow</title>
                    </head>

                    <body style="
                        margin:0;
                        padding:0;
                        background-color:#171717;
                        font-family:Arial,Helvetica,sans-serif;
                    ">

                    <table role="presentation"
                           width="100%"
                           cellspacing="0"
                           cellpadding="0"
                           border="0"
                           style="background-color:#171717;padding:20px 8px;">

                        <tr>
                            <td align="center">

                                <table role="presentation"
                                       width="100%"
                                       cellspacing="0"
                                       cellpadding="0"
                                       border="0"
                                       style="
                                           max-width:710px;
                                           border:1px solid #d9d9d9;
                                           border-radius:22px;
                                           overflow:hidden;
                                           background-color:#202123;
                                       ">

                                    <!-- TOP ACCENT -->

                                    <tr>
                                        <td height="12"
                                            style="background-color:#b6421d;">
                                        </td>
                                    </tr>

                                    <!-- HEADER -->

                                    <tr>
                                        <td style="
                                            background-color:#e7ebf5;
                                            padding:55px 8%;
                                        ">

                                            <div style="
                                                font-size:42px;
                                                font-weight:800;
                                                letter-spacing:-2px;
                                                color:#202124;
                                                line-height:1.1;
                                            ">
                                                Mail<span style="color:#d65d35;">flow</span>
                                            </div>

                                            <div style="
                                                margin-top:18px;
                                                font-size:19px;
                                                font-weight:700;
                                                letter-spacing:4px;
                                                line-height:1.5;
                                                color:#55585f;
                                            ">
                                                INTELLIGENT EMAIL<br>
                                                COMMUNICATION
                                            </div>

                                        </td>
                                    </tr>

                                    <!-- EMAIL BODY -->

                                    <tr>
                                        <td style="
                                            background-color:#202123;
                                            padding:65px 9%;
                                            color:#ffffff;
                                            font-size:19px;
                                            line-height:1.9;
                                            word-break:break-word;
                                        ">

                                            {{EMAIL_BODY}}

                                        </td>
                                    </tr>

                                    <!-- FOOTER -->

                                    <tr>
                                        <td style="
                                            background-color:#292719;
                                            padding:55px 25px 60px;
                                            text-align:center;
                                            border-top:1px solid #d0d0d0;
                                        ">

                                            <div style="
                                                color:#ffffff;
                                                font-size:21px;
                                                font-weight:bold;
                                                margin-bottom:18px;
                                            ">
                                                Stay connected with Mailflow
                                            </div>

                                            <div style="
                                                color:#b7b5ae;
                                                font-size:16px;
                                                line-height:1.8;
                                                margin-bottom:38px;
                                            ">
                                                You are receiving this email because
                                                you are subscribed to our mailing list.
                                            </div>

                                            <table role="presentation"
                                                   cellspacing="0"
                                                   cellpadding="0"
                                                   border="0"
                                                   align="center">

                                                <tr>
                                                    <td align="center"
                                                        bgcolor="#b6421d"
                                                        style="
                                                            border:4px solid #e7ebf5;
                                                            border-radius:16px;
                                                        ">

                                                        <a href="{{UNSUBSCRIBE_URL}}"
                                                           target="_blank"
                                                           style="
                                                               display:inline-block;
                                                               padding:23px 48px;
                                                               color:#ffffff;
                                                               text-decoration:none;
                                                               font-size:19px;
                                                               font-weight:bold;
                                                               letter-spacing:2px;
                                                               border-radius:12px;
                                                           ">
                                                            UNSUBSCRIBE
                                                        </a>

                                                    </td>
                                                </tr>

                                            </table>

                                            <div style="
                                                color:#858276;
                                                font-size:12px;
                                                margin-top:35px;
                                            ">
                                                © Mailflow | Intelligent Email Communication
                                            </div>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>

                    </table>

                    </body>
                    </html>

                    """;

            // 5. REPLACE HTML PLACEHOLDERS

            htmlContent = htmlContent
                    .replace("{{EMAIL_BODY}}", body)
                    .replace(
                            "{{UNSUBSCRIBE_URL}}",
                            html(unsubscribeUrl)
                    );

            // 6. CREATE MIME MESSAGE

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(sender);
            helper.setTo(user.getEmail());

            // 7. PERSONALIZE SUBJECT

            String personalizedSubject =
                    subject == null ? "" : subject;

            personalizedSubject = personalizedSubject.replace(
                    "{{name}}",
                    user.getName() == null ? "" : user.getName()
            );

            helper.setSubject(personalizedSubject);

            // 8. SET HTML EMAIL

            helper.setText(htmlContent, true);

            // =================================================
            // 9. ADD MULTIPLE PDF AND IMAGE ATTACHMENTS
            // =================================================

            if (attachments != null && !attachments.isEmpty()) {

                for (AttachmentData attachment : attachments) {

                    if (attachment == null ||
                            attachment.content() == null ||
                            attachment.content().length == 0) {

                        continue;
                    }

                    String filename = attachment.filename();

                    if (filename == null || filename.isBlank()) {
                        filename = "attachment";
                    }

                    String contentType = attachment.contentType();

                    if (contentType == null ||
                            contentType.isBlank() ||
                            contentType.equals("application/octet-stream")) {

                        contentType = detectContentType(filename);
                    }

                    helper.addAttachment(
                            filename,
                            new ByteArrayResource(attachment.content()),
                            contentType
                    );

                    System.out.println(
                            "Attachment added: " + filename
                    );
                }
            }

            // 10. SEND EMAIL

            mailSender.send(message);

            System.out.println(
                    "Template email sent successfully to: "
                            + user.getEmail()
            );

        } catch (Exception ex) {

            System.err.println(
                    "Email failed for " +
                            (user == null ? "unknown user" : user.getEmail()) +
                            ": " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    // =====================================================
    // DETECT FILE CONTENT TYPE
    // =====================================================

    private String detectContentType(String filename) {

        String name = filename.toLowerCase();

        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }

        if (name.endsWith(".png")) {
            return "image/png";
        }

        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (name.endsWith(".gif")) {
            return "image/gif";
        }

        if (name.endsWith(".webp")) {
            return "image/webp";
        }

        return "application/octet-stream";
    }

    // =====================================================
    // HTML ESCAPING
    // =====================================================

    private String html(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}