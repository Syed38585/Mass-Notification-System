package com.project.mailSender.Controller;

import com.project.mailSender.Model.CountryEmailRequest;
import com.project.mailSender.Model.CountryEmailResponse;
import com.project.mailSender.Model.CountryEmailResult;
import com.project.mailSender.Service.CountryEmailService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class CountryEmailController {

    private final CountryEmailService countryEmailService;

    public CountryEmailController(
            CountryEmailService countryEmailService) {

        this.countryEmailService = countryEmailService;
    }

    @PostMapping("/send/by-country")
    public ResponseEntity<CountryEmailResult> sendByCountry(
            @Valid @RequestBody CountryEmailRequest request) {

        CountryEmailResult response =
                countryEmailService.sendToCountry(request.country(),request.subject(),request.body());

        return ResponseEntity.ok(response);
    }
}