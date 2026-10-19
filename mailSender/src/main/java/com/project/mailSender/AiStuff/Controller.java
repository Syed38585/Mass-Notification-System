package com.project.mailSender.AiStuff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class Controller {

    @Autowired
    AiService service;

    @GetMapping("/get")
    public Flux<String> getResponse(@RequestParam String prompt){
        return service.getResponse(prompt);
    }

//    @PostMapping("/recipients")
//    public void getRecipients(@RequestBody String request) throws Exception {
//
//         service.getRecipients(request);
//    }
}
