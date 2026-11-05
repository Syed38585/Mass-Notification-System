package com.project.mailSender.Model;

public record CountryEmailResult(
        String country,
        int usersFound,
        int emailsQueued,
        String message
) {}