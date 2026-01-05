package com.example.minibank2.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NumberGeneratorService {

    public String generateAccountNumber() {
        // Możesz zmienić format później, np. PL + random digits
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

