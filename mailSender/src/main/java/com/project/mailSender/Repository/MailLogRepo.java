package com.project.mailSender.Repository;

import com.project.mailSender.Model.MailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MailLogRepo extends JpaRepository<MailLog, Long> {

    @Query("SELECT COALESCE(AVG(m.durationMs),0) FROM MailLog m")
    long findAverageDuration();

    Optional<MailLog> findTopByOrderBySentAtDesc();

    List<MailLog> findBySentAtAfter(LocalDateTime time);

}