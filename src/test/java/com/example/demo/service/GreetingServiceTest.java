package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {

    private final GreetingService greetingService = new GreetingService();

    @Test
    @DisplayName("이름이 주어지면 정상적인 인사말을 반환한다")
    void testGenerateGreetingWithName() {
        assertEquals("Hello, Ji-eun!", greetingService.generateGreeting("Ji-eun"));
    }

    @Test
    @DisplayName("이름이 null이거나 비어있으면 Guest로 인사한다")
    void testGenerateGreetingWithEmptyName() {
        assertEquals("Hello, Guest!", greetingService.generateGreeting(null));
        assertEquals("Hello, Guest!", greetingService.generateGreeting("   "));
    }
}