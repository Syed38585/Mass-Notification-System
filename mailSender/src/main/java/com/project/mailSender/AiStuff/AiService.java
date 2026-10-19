package com.project.mailSender.AiStuff;

import com.project.mailSender.Service.UserService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiService {

//    @Value("${groq.api.key}")
//    private String apiKey;
//
//    @Value("${groq.api.url}")
//    private String apiUrl;
//

        private final ChatClient chatClient;

        @Autowired
        UserService service;

        public AiService(ChatClient.Builder builder) {
            this.chatClient = builder.build();
        }

        public String chat(String prompt) {
            return chatClient.prompt(prompt)
                    .call()
                    .content();
        }

//    public List<UserDto> getRecipients(String prompt) throws Exception {
//
//        System.out.println("Step 1");
//
//        List<Users> users = service.getAll();
//
//        System.out.println("Step 2");
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        String usersJson = mapper.writeValueAsString(users);
//
//        System.out.println("Step 3");
//
//        String finalPrompt = """
//    You are given these users.
//
//    %s
//
//    User Request:
//
//    %s
//
//Return ONLY a JSON array.
//
//Each object MUST contain exactly these fields:
//
//[
//  {
//    "id": 1,
//    "name": "John",
//    "email": "john@gmail.com"
//  }
//]
//
//Do not include gender.
//Do not include dob.
//Do not include prime.
//    """.formatted(usersJson, prompt);
//
//        System.out.println("Step 4");
//        System.out.println(finalPrompt);
//
//        String response = chatClient.prompt(finalPrompt)
//                .call()
//                .content();
//
//        System.out.println("Step 5");
//
//        System.out.println(response);
//
//        return mapper.readValue(
//                response,
//                new com.fasterxml.jackson.core.type.TypeReference<List<UserDto>>() {}
//        );
//    }
}

