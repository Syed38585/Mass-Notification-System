package com.project.mailSender.Service;

import com.project.mailSender.Model.MailRecord;
import com.project.mailSender.Model.MailRecordDTO;
import com.project.mailSender.Repository.MailRecordRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MailRecordService {

    private final MailRecordRepository repository;

    public MailRecordService(
            MailRecordRepository repository) {
        this.repository = repository;
    }

    public List<MailRecord> getRecordsByStatus(String status) {
        return repository.findByStatusIgnoreCase(status);
    }

    // GET HISTORY
    public List<MailRecord> getRecords(String status) {

        if (status == null || status.isBlank()) {
            return repository.findAllByOrderByUpdatedAtDesc();
        }

        List<String> statuses = Arrays.stream(
                        status.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (statuses.isEmpty()) {
            return List.of();
        }

        return repository
                .findByStatusInOrderByUpdatedAtDesc(statuses);
    }

    public List<MailRecord> getAllRecords() {
        return repository.findAll();
    }

    // SAVE DRAFT
    @Transactional
    public MailRecord saveDraft(
            Long id,
            String subject,
            String content,
            String targetGroup,
            String recipientIds) {

        MailRecord record;

        if (id != null) {

            record = repository.findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Mail record not found"
                            ));

            if (!"DRAFT".equals(record.getStatus())) {
                throw new IllegalArgumentException(
                        "Only drafts can be updated"
                );
            }

        } else {
            record = new MailRecord();
        }

        record.setSubject(subject);
        record.setContent(content);
        record.setTargetGroup(targetGroup);
        record.setRecipientIds(recipientIds);
        record.setStatus("DRAFT");

        return repository.saveAndFlush(record);
    }

    @Transactional
    public MailRecord saveScheduledRecord(MailRecordDTO dto) {

        MailRecord record = new MailRecord();

        record.setSubject(dto.getSubject());
        record.setContent(dto.getContent());
        record.setTargetGroup(dto.getTargetGroup());
        record.setRecipientIds(dto.getRecipientIds());
        record.setRecipientNames(dto.getRecipientNames());
        record.setRecipientCount(dto.getRecipientCount());
        record.setHasAttachments(dto.getHasAttachments());

        record.setStatus("SCHEDULED");
        record.setScheduledAt(dto.getScheduledAt());

        return repository.save(record);
    }


    @Transactional
    public void updateStatus(Long id, String status) {

        MailRecord record = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Mail record not found"));

        record.setStatus(status);

        if ("SENT".equalsIgnoreCase(status)) {
            record.setSentAt(LocalDateTime.now());
        }

        repository.save(record);
    }

    @Transactional
    public MailRecord saveRecord(MailRecordDTO dto) {


        MailRecord record = new MailRecord();

        record.setSubject(dto.getSubject());
        record.setContent(dto.getContent());
        record.setTargetGroup(dto.getTargetGroup());
        record.setRecipientIds(dto.getRecipientIds());
        record.setRecipientNames(dto.getRecipientNames());
        record.setRecipientCount(dto.getRecipientCount());
        record.setHasAttachments(dto.getHasAttachments());

        record.setStatus(dto.getStatus());

        record.setSentAt(
                dto.getSentAt() != null
                        ? dto.getSentAt()
                        : LocalDateTime.now()
        );

        record.setDurationMs(dto.getDurationMs());

        return repository.saveAndFlush(record);
    }

    // SAVE SENT CAMPAIGN
    @Transactional
    public MailRecord saveSentRecord(
            String subject,
            String content,
            String targetGroup,
            String recipientIds,
            String recipientNames,
            int recipientCount,
            boolean hasAttachments,
            long durationMs) {

        MailRecord record = new MailRecord();

        record.setSubject(subject);
        record.setContent(content);
        record.setTargetGroup(targetGroup);
        record.setRecipientIds(recipientIds);
        record.setRecipientNames(recipientNames);
        record.setRecipientCount(recipientCount);
        record.setHasAttachments(hasAttachments);

        record.setStatus("SENT");
        record.setSentAt(LocalDateTime.now());
        record.setDurationMs(durationMs);

        return repository.saveAndFlush(record);
    }

    // SAVE SCHEDULED CAMPAIGN
    @Transactional
    public MailRecord saveScheduledRecord(
            String subject,
            String content,
            String targetGroup,
            String recipientIds,
            String recipientNames,
            int recipientCount,
            boolean hasAttachments,
            LocalDateTime sendAt) {

        MailRecord record = new MailRecord();

        record.setSubject(subject);
        record.setContent(content);
        record.setTargetGroup(targetGroup);
        record.setRecipientIds(recipientIds);
        record.setRecipientNames(recipientNames);
        record.setRecipientCount(recipientCount);
        record.setHasAttachments(hasAttachments);

        record.setStatus("SCHEDULED");
        record.setSendAt(sendAt);

        return repository.saveAndFlush(record);
    }

    // CANCEL SCHEDULED EMAIL
    @Transactional
    public void cancelScheduled(Long id) {

        MailRecord record = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Mail record not found"
                        ));

        if (!"SCHEDULED".equals(record.getStatus())) {
            throw new IllegalStateException(
                    "Only scheduled emails can be cancelled"
            );
        }

        record.setStatus("CANCELLED");

        repository.saveAndFlush(record);
    }

    // DELETE HISTORY
    @Transactional
    public void deleteRecord(Long id) {

        MailRecord record = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Mail record not found"
                        ));

        repository.delete(record);
    }
}