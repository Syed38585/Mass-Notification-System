package com.project.mailSender.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mailSender.MailService.MailService;
import com.project.mailSender.Model.MailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@CrossOrigin(origins = "*")
public class MailController {

    @Autowired
    private MailService mailService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(value = "/dispatch", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> dispatchMail(

            @ModelAttribute MailRequest mailRequest,

            @RequestParam(required = false) String gender,

            @RequestParam(required = false) List<Long> recipientIds,

            @RequestParam(required = false) MultipartFile image,

            @RequestParam(required = false) MultipartFile pdf

    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            if (mailRequest.getSubject() == null ||
                    mailRequest.getSubject().isBlank()) {

                return ResponseEntity.badRequest()
                        .body(error("Subject cannot be empty"));
            }

            if (mailRequest.getContent() == null ||
                    mailRequest.getContent().isBlank()) {

                return ResponseEntity.badRequest()
                        .body(error("Content cannot be empty"));
            }

            if (mailRequest.getSendAt() != null &&
                    mailRequest.getSendAt().isBefore(LocalDateTime.now())) {

                return ResponseEntity.badRequest()
                        .body(error("Scheduled time must be in the future"));
            }

            // IMPORTANT
            // Convert repeated recipientIds parameters into MailRequest
            if (recipientIds != null && !recipientIds.isEmpty()) {
                mailRequest.setRecipientIds(recipientIds);
            }

            byte[] imageBytes = null;
            byte[] pdfBytes = null;

            String imageName = null;
            String pdfName = null;

            if (image != null && !image.isEmpty()) {
                imageBytes = image.getBytes();
                imageName = image.getOriginalFilename();
            }

            if (pdf != null && !pdf.isEmpty()) {
                pdfBytes = pdf.getBytes();
                pdfName = pdf.getOriginalFilename();
            }

            mailService.processDispatch(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

            response.put("status", "SUCCESS");

            if (mailRequest.getRecipientIds() != null &&
                    !mailRequest.getRecipientIds().isEmpty()) {

                response.put("target", "Selected Users");
                response.put(
                        "selectedCount",
                        mailRequest.getRecipientIds().size()
                );

            } else if (gender != null && !gender.isBlank()
                    && !gender.equalsIgnoreCase("all")) {

                response.put("target", gender);

            } else {

                response.put("target", "All Users");
            }

            if (mailRequest.getSendAt() == null) {

                response.put(
                        "message",
                        "Emails are being sent."
                );

            } else {

                response.put(
                        "message",
                        "Email scheduled successfully."
                );

                response.put(
                        "scheduledFor",
                        mailRequest.getSendAt()
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(error(
                            "Failed to process mail request : "
                                    + e.getMessage()
                    ));
        }
    }

    private Map<String, Object> error(String message) {

        Map<String, Object> map = new HashMap<>();

        map.put("status", "ERROR");
        map.put("message", message);

        return map;
    }
}