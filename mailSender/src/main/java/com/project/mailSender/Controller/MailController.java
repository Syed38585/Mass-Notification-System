package com.project.mailSender.Controller;

import com.project.mailSender.MailService.MailService;
import com.project.mailSender.Model.MailRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@CrossOrigin(origins = "*")
public class MailController {

    @Autowired
    private MailService mailService;

    @PostMapping(value = "/dispatch", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> dispatchMail(

            @ModelAttribute MailRequest mailRequest,
            @RequestParam(required = false) String gender,

            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile pdf
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            // =============================
            // Validate subject
            // =============================
            if (mailRequest.getSubject() == null || mailRequest.getSubject().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(error("Subject cannot be empty"));
            }

            // =============================
            // Validate content
            // =============================
            if (mailRequest.getContent() == null || mailRequest.getContent().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(error("Content cannot be empty"));
            }

            // =============================
            // Validate scheduled time
            // =============================
            if (mailRequest.getSendAt() != null) {

                if (mailRequest.getSendAt().isBefore(LocalDateTime.now())) {
                    return ResponseEntity.badRequest()
                            .body(error("Scheduled time must be in the future"));
                }
            }

            // =============================
            // Convert files to byte[]
            // =============================
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

            // =============================
            // Call service
            // =============================
            mailService.processDispatch(
                    mailRequest,
                    gender,
                    imageBytes,
                    imageName,
                    pdfBytes,
                    pdfName
            );

            response.put("status", "SUCCESS");

            if (mailRequest.getSendAt() == null) {

                response.put("message", "Emails are being sent immediately.");

            } else {

                response.put("message", "Email scheduled successfully.");
                response.put("scheduledFor", mailRequest.getSendAt());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(error("Failed to process mail request: " + e.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {

        Map<String, Object> error = new HashMap<>();

        error.put("status", "ERROR");
        error.put("message", message);

        return error;
    }
}