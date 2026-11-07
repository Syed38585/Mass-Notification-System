
package com.project.mailSender.Controller;

import com.project.mailSender.Model.DraftRequest;
import com.project.mailSender.Model.MailRecord;
import com.project.mailSender.Service.MailRecordService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
public class MailRecordController {

    private final MailRecordService service;

    public MailRecordController(MailRecordService service) {
        this.service = service;
    }

    // SENT, DRAFT, SCHEDULED, MISSED, CANCELLED, FAILED

    @GetMapping("/records")
    public ResponseEntity<List<MailRecord>> getRecords(
            @RequestParam(required = false) String status
    ) {

        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(
                    service.getRecordsByStatus(status)
            );
        }

        return ResponseEntity.ok(
                service.getAllRecords()
        );
    }

    // Delete mail record

    @DeleteMapping("/records/{id}")
    public ResponseEntity<?> deleteRecord(
            @PathVariable Long id
    ) {

        service.deleteRecord(id);

        return ResponseEntity.ok(
                Map.of("message", "Mail record deleted successfully")
        );
    }

    // Cancel scheduled mail

    @PostMapping("/scheduled/{id}/cancel")
    public ResponseEntity<?> cancelScheduled(
            @PathVariable Long id
    ) {

        service.cancelScheduled(id);

        return ResponseEntity.ok(
                Map.of("message", "Scheduled mail cancelled")
        );
    }

    // Save draft

    @PostMapping("/drafts")
    public ResponseEntity<MailRecord> saveDraft(
            @RequestBody DraftRequest request) {

        MailRecord record = service.saveDraft(
                request.getId(),
                request.getSubject(),
                request.getContent(),
                request.getTargetGroup(),
                request.getRecipientIds()
        );

        return ResponseEntity.ok(record);
    }
}
