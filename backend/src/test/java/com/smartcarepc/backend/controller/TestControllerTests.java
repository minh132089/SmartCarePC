package com.smartcarepc.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Chỉ dựng tầng web, không cần MySQL
@WebMvcTest(TestController.class)
class TestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingTraVeOk() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void echoTraLaiDungBody() throws Exception {
        mockMvc.perform(post("/api/test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hello\":\"SmartCarePC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received.hello").value("SmartCarePC"));
    }
}
