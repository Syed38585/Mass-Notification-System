package com.project.mailSender.AiStuff;

import com.project.mailSender.MailService.MailSenderClass;
import com.project.mailSender.MailService.MailService;
import com.project.mailSender.Model.MailLog;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.MailLogRepo;
import com.project.mailSender.Repository.Repo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EmailTool {

    @Autowired
    private Repo userRepository;

    @Autowired
    private MailSender sendMail;

    @Value("${spring.mail.username}")
    private String sender;

    @Autowired
    private MailLogRepo mailLogRepo;

    @Autowired
    private MailSenderClass mailService;

//    @Async("mailExecutor")
    @Tool(
            description = "Send an email to a recipient. " +
                    "The email parameter is the recipient's email address " +
                    "and the text parameter is the complete email content."
    )
    public String sendEmail(
            @ToolParam(description = "The subject of the mail") String subject,
            @ToolParam(description = "The id of the user") Long id,
            @ToolParam(description = "The text given by the Admin") String text) {

//        System.out.println("EMAIL TOOL HIT");
//
//        long start = System.currentTimeMillis();
//
//
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//
//        mailMessage.setSubject(subject);
//        mailMessage.setFrom(sender);
//        mailMessage.setTo(email);
//        mailMessage.setText(text);
//
//        sendMail.send(mailMessage);
//
//        long duration = System.currentTimeMillis() - start;
//
//        MailLog log = new MailLog();
//        log.setTargetGroup("ai-tool");
//        log.setSentAt(LocalDateTime.now());
//        log.setDurationMs(duration);
//        log.setTotalRecipients(1);
//
//        mailLogRepo.save(log);
//
        Users users = userRepository.findById(id).orElseThrow();
        mailService.sendEmailAsync(users,subject,text,null,null,null,null);
        return "DONE";
    }
}