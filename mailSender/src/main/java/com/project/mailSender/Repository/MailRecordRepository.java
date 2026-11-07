package com.project.mailSender.Repository;

import com.project.mailSender.Model.MailRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailRecordRepository
        extends JpaRepository<MailRecord, Long> {

    List<MailRecord> findByStatusInOrderByUpdatedAtDesc(
            List<String> statuses
    );

    List<MailRecord> findByStatusOrderByUpdatedAtDesc(
            String status
    );

    List<MailRecord> findAllByOrderByUpdatedAtDesc();

    List<MailRecord> findByStatusIgnoreCase(String status);
}