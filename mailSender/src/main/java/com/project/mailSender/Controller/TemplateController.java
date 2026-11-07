package com.project.mailSender.Controller;

import com.project.mailSender.Model.TemplateSendRequest;
import com.project.mailSender.Service.TemplateMailService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {

    private final TemplateMailService templateMailService;

    public TemplateController(TemplateMailService templateMailService) {
        this.templateMailService = templateMailService;
    }

    @PostMapping(
            value = "/send",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> send(

            @Valid
            @RequestPart("request")
            TemplateSendRequest request,

            @RequestPart(value = "files", required = false)
            MultipartFile[] files,

            @RequestParam(value = "gender", required = false)
            String gender,

            @RequestParam(value = "recipientIds", required = false)
            List<Long> recipientIds
    ) {

        try {

            int queued = templateMailService.sendToSubscribedUsers(
                    request,
                    files,
                    gender,
                    recipientIds
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    Map.of(
                            "success", true,
                            "queuedRecipients", queued,
                            "message", "Template emails queued successfully"
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "success", false,
                            "message", "Failed to send template emails",
                            "error", e.getMessage()
                    )
            );
        }
    }
}