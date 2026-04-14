package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    public String generateGreeting(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, Guest!";
        }
        return "Hello, " + name + "!";
    }
}