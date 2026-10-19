package com.project.mailSender.AiStuff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class Controller {

    @Autowired
    AiService service;

    @GetMapping("/get")
    public ResponseEntity<String> getResponse(@RequestParam String prompt){
        return new ResponseEntity<>(service.chat(prompt), HttpStatus.OK);
    }
}
