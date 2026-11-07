package com.project.mailSender.MailService;

import com.project.mailSender.Model.MailRecordDTO;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Service.JwtService;
import com.project.mailSender.Service.MailRecordService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.mail.javamail.JavaMailSender;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Component
public class MailSenderClass {

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    MailRecordService mailRecordService;

    @Autowired
    JwtService jwtService;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.base-url}")
    private String baseUrl;

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

            if (user == null) {
                System.err.println("Skipping email because user is null");
                return;
            }

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

            String token = jwtService.generateUnsubscribeToken(user.getId());


            String unsubscribeUrl =
                    baseUrl
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
                            width:100%;
                            text-align:center;
                        ">

                            <img
                                src="cid:mailImage"
                                alt="Attached Image"
                                style="
                                    display:block;
                                    width:100%;
                                    max-width:100%;
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

                                {{PDF_DISPLAY_NAME}}

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

                        """;

                pdfSection = pdfSection.replace(
                        "{{PDF_DISPLAY_NAME}}",
                        html(displayName)
                );
            }


            // =================================================
            // 8. COMPLETE EMAIL HTML
            // =================================================

            // =================================================
// 8. COMPLETE EMAIL HTML - MAILFLOW DESIGN
// =================================================

            // =================================================
// 8. COMPLETE EMAIL HTML - MAILFLOW DESIGN
// =================================================

            String html = """

        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="light">
            <title>Mailflow</title>
        </head>

        <body style="
            margin:0;
            padding:0;
            background-color:#f3f4f6;
            font-family:Arial,Helvetica,sans-serif;
            color:#14181f;
        ">

        <!-- OUTER BACKGROUND -->
        <table
            role="presentation"
            width="100%"
            cellpadding="0"
            cellspacing="0"
            border="0"
            style="
                width:100%;
                background-color:#f3f4f6;
                padding:35px 12px;
            "
        >

            <tr>
                <td align="center">

                    <!-- MAIN EMAIL CARD -->
                    <table
                        role="presentation"
                        width="600"
                        cellpadding="0"
                        cellspacing="0"
                        border="0"
                        style="
                            width:100%;
                            max-width:600px;
                            background-color:#ffffff;
                            border:2px solid #14181f;
                            border-radius:12px;
                            box-shadow:6px 6px 0 #14181f;
                            overflow:hidden;
                        "
                    >

                        <!-- ORANGE TOP ACCENT -->
                        <tr>
                            <td style="
                                height:8px;
                                background-color:#ff6b3d;
                                font-size:0;
                                line-height:0;
                            ">
                                &nbsp;
                            </td>
                        </tr>


                        <!-- HEADER -->
                        <tr>
                            <td style="
                                padding:25px 28px;
                                background-color:#14181f;
                            ">

                                <table
                                    role="presentation"
                                    width="100%"
                                    cellpadding="0"
                                    cellspacing="0"
                                    border="0"
                                >

                                    <tr>

                                        <!-- BRAND NAME -->
                                        <td align="left" valign="middle">

                                            <div style="
                                                font-size:28px;
                                                font-weight:900;
                                                letter-spacing:-1px;
                                                color:#ffffff;
                                            ">
                                                Mail<span style="
                                                    color:#ff6b3d;
                                                ">flow</span>
                                            </div>

                                            <div style="
                                                margin-top:5px;
                                                font-size:10px;
                                                font-weight:bold;
                                                letter-spacing:2px;
                                                color:#d1d5db;
                                            ">
                                                INTELLIGENT EMAIL COMMUNICATION
                                            </div>

                                        </td>


                                        <!-- AI BADGE -->
                                        <td align="right" valign="middle">

                                            <span style="
                                                display:inline-block;
                                                padding:9px 13px;
                                                background-color:#ffd23f;
                                                color:#14181f;
                                                border:2px solid #14181f;
                                                border-radius:6px;
                                                font-size:10px;
                                                font-weight:900;
                                                letter-spacing:1px;
                                            ">
                                                MAILFLOW AI
                                            </span>

                                        </td>

                                    </tr>

                                </table>

                            </td>
                        </tr>


                        <!-- MESSAGE CONTENT -->
                        <tr>
                            <td style="
                                padding:35px 30px;
                                color:#14181f;
                                font-size:{{TEXT_SIZE}}px;
                                line-height:1.8;
                                word-wrap:break-word;
                                overflow-wrap:break-word;
                                background-color:#ffffff;
                            ">

                                <!-- EMAIL MESSAGE -->
                                <div style="
                                    width:100%;
                                    font-size:{{TEXT_SIZE}}px;
                                    line-height:1.8;
                                    color:#14181f;
                                ">

                                    {{MESSAGE_CONTENT}}

                                </div>


                                <!-- INLINE IMAGE -->
                                {{IMAGE_SECTION}}


                                <!-- PDF ATTACHMENT INFORMATION -->
                                {{PDF_SECTION}}

                            </td>
                        </tr>


                        <!-- DIVIDER -->
                        <tr>
                            <td style="
                                padding:0 28px;
                                background-color:#ffffff;
                            ">

                                <div style="
                                    height:2px;
                                    background-color:#14181f;
                                    font-size:0;
                                    line-height:0;
                                ">
                                    &nbsp;
                                </div>

                            </td>
                        </tr>


                        <!-- UNSUBSCRIBE SECTION -->
                        <tr>
                            <td align="center" style="
                                padding:28px 25px;
                                background-color:#fff9e6;
                            ">

                                <p style="
                                    margin:0 0 8px;
                                    font-size:13px;
                                    font-weight:bold;
                                    color:#14181f;
                                ">
                                    Stay connected with Mailflow
                                </p>

                                <p style="
                                    margin:0 0 18px;
                                    font-size:12px;
                                    line-height:1.6;
                                    color:#555555;
                                ">
                                    You are receiving this email because
                                    you are subscribed to our mailing list.
                                </p>

                                <a
                                    href="{{UNSUBSCRIBE_URL}}"
                                    style="
                                        display:inline-block;
                                        padding:12px 25px;
                                        background-color:#ff6b3d;
                                        color:#14181f;
                                        border:2px solid #14181f;
                                        border-radius:7px;
                                        text-decoration:none;
                                        font-size:13px;
                                        font-weight:900;
                                        letter-spacing:0.5px;
                                    "
                                >
                                    UNSUBSCRIBE
                                </a>

                            </td>
                        </tr>


                        <!-- FOOTER -->
                        <tr>
                            <td align="center" style="
                                padding:22px 25px;
                                background-color:#14181f;
                            ">

                                <div style="
                                    font-size:19px;
                                    font-weight:900;
                                    color:#ffffff;
                                ">
                                    Mail<span style="
                                        color:#ff6b3d;
                                    ">flow</span>
                                </div>

                                <p style="
                                    margin:8px 0 0;
                                    font-size:10px;
                                    line-height:1.6;
                                    color:#d1d5db;
                                ">
                                    Powered by intelligent email technology.
                                </p>

                                <p style="
                                    margin:12px 0 0;
                                    font-size:10px;
                                    color:#9ca3af;
                                ">
                                    © 2026 Mailflow. All rights reserved.
                                </p>

                            </td>
                        </tr>

                    </table>


                    <!-- BOTTOM TEXT -->
                    <p style="
                        margin:22px 0 0;
                        font-size:10px;
                        color:#6b7280;
                        text-align:center;
                    ">
                        Sent with Mailflow AI
                    </p>

                </td>
            </tr>

        </table>

        </body>
        </html>

        """;

            // Replace only explicit placeholders. No String.formatted() is used,
            // so literal CSS/HTML values such as width:100% remain unchanged.
            html = html
                    .replace("{{TEXT_SIZE}}", String.valueOf(textSize))
                    .replace("{{MESSAGE_CONTENT}}", formattedContent)
                    .replace("{{IMAGE_SECTION}}", imageSection)
                    .replace("{{PDF_SECTION}}", pdfSection)
                    .replace("{{UNSUBSCRIBE_URL}}", unsubscribeUrl);


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

            long sendStartedAt = System.currentTimeMillis();
            mailSender.send(message);
            long durationMs = System.currentTimeMillis() - sendStartedAt;

            System.out.println(
                    "Email sent successfully to " + user.getEmail()
            );

            // =================================================
            // 14. SAVE HISTORY ONLY AFTER SMTP SEND SUCCEEDS
            // =================================================

            try {
                MailRecordDTO record = new MailRecordDTO();
                record.setSubject(subject);
                record.setContent(content);
                record.setTargetGroup(user.getName());
                record.setRecipientIds(
                        user.getId() != null ? user.getId().toString() : null
                );
                record.setRecipientNames(user.getEmail());
                record.setRecipientCount(1);
                record.setHasAttachments(
                        (imageBytes != null && imageBytes.length > 0)
                                || (pdfBytes != null && pdfBytes.length > 0)
                );
                record.setStatus("SENT");
                record.setSentAt(java.time.LocalDateTime.now());
                record.setDurationMs(durationMs);

                mailRecordService.saveRecord(record);

                System.out.println(
                        "Email history saved successfully for " + user.getEmail()
                );

            } catch (Exception historyException) {
                // The email has already been sent. Do not report it as a send failure.
                System.err.println(
                        "Email was sent, but history could not be saved for "
                                + user.getEmail()
                );
                historyException.printStackTrace();
            }


        } catch (Exception e) {

            System.err.println(
                    "Failed to send email to "
                            + (user != null ? user.getEmail() : "unknown recipient")
            );

            e.printStackTrace();
        }
    }

    private String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

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

}
