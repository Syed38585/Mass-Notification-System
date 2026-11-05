package com.project.mailSender.AiStuff;

import com.project.mailSender.Model.AiLog;
import com.project.mailSender.Repository.AiLogRepo;
import com.project.mailSender.Service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

//    @Value("${groq.api.key}")
//    private String apiKey;
//
//    @Value("${groq.api.url}")
//    private String apiUrl;
//

    private final ChatClient chatClient;

    private final GetUserInfo getUserInfo;

    private final EmailTool emailTool;

    private final AnalyticsAgent analyticsAgent;


    private final AiLogRepo aiLogRepo;

    List<Message> list = new ArrayList<>();

    public AiService(ChatClient.Builder builder, GetUserInfo getUserInfo, EmailTool emailTool, AnalyticsAgent analyticsAgent, AiLogRepo aiLogRepo) {
        this.chatClient = builder.build();
        this.getUserInfo = getUserInfo;
        this.emailTool = emailTool;
        this.analyticsAgent = analyticsAgent;
        this.aiLogRepo = aiLogRepo;
    }

    String SYSTEM_PROMPT = """
            Role = You are an AI assistant at MAIL PILOT AI, a platform used to send emails.and also have access to Analytics tools
            
            Task = Your task is to provide the information requested by the admin. You have access to tools such as user information retrieval and email sending. You can send emails to users specified by the admin. When the admin provides a message, retrieve the users who match the given criteria, professionally polish the message while preserving its original meaning, and send it to the appropriate users.
            and even if the user is not available in repo use the email specified by user to send mail
            and at the end of every mail if the user has provided the name use it in regards or use Admin @ Mail Pilot AI Team
            
            The email-sending process should run in the background. Do not wait for the email process to finish before responding to the admin. Respond immediately after the email-sending process has been initiated and allow the email-sending task to continue in the background.
            
            Behaviour = Your behaviour should always be professional, clear, and positive.
            
            Constraint = Do not respond in an unprofessional manner. Always be honest about the status of the requested operation and never claim that an email has been sent unless the sending process has actually been initiated.
            """;



    public Flux<String> getResponse(String prompt) {

        list.add(new UserMessage(prompt));

        StringBuilder stringBuilder = new StringBuilder();

        AiLog log = new AiLog();
        log.setRequestedAt(LocalDateTime.now());
        aiLogRepo.save(log);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(list)
                .tools(getUserInfo, emailTool, analyticsAgent)
                .stream()
                .content()
                .doOnNext(stringBuilder::append)
                .doOnComplete(() -> list.add(new AssistantMessage(stringBuilder.toString())));
    }
}