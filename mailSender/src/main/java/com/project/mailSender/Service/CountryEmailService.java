package com.project.mailSender.Service;

import com.project.mailSender.MailService.MailSenderClass;
import com.project.mailSender.Model.CountryEmailResult;
import com.project.mailSender.Model.Users;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CountryEmailService {

    private final UserService userService;
    private final MailSenderClass mailSenderClass;

    public CountryEmailService(
            UserService userService,
            MailSenderClass mailSenderClass) {
        this.userService = userService;
        this.mailSenderClass = mailSenderClass;
    }

    public CountryEmailResult sendToCountry(
            String country,
            String subject,
            String body) {

        List<Users> users =
                userService.getUsersByCountry(country.trim());

        int eligible = 0;

        for (Users user : users) {

            // Send only to subscribed users with an email address.
            if (!user.isSubscribed()
                    || user.getEmail() == null
                    || user.getEmail().isBlank()) {
                continue;
            }

            eligible++;

            // Reuse your existing email implementation.
            // No image or PDF attachments for this campaign.
            mailSenderClass.sendEmailAsync(
                    user,
                    subject,
                    body,
                    null,  // imageBytes
                    null,  // imageName
                    null,  // pdfBytes
                    null   // pdfName
            );
        }

        return new CountryEmailResult(
                country.trim(),
                users.size(),
                eligible,
                "Email sending queued for eligible subscribed users"
        );
    }
}