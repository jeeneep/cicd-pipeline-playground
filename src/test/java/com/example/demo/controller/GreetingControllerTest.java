package com.example.demo.controller;

import com.example.demo.service.GreetingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GreetingService greetingService;

    @Test
    @DisplayName("이름 파라미터가 있을 때 정상 응답을 반환한다")
    void testGreet() throws Exception {
        when(greetingService.generateGreeting("Ji-eun")).thenReturn("Hello, Ji-eun!");

        mockMvc.perform(get("/api/greetings").param("name", "Ji-eun"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Ji-eun!"));
    }
}