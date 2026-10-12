package com.smartcarepc.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.FeedbackSaved;
import com.smartcarepc.backend.service.FeedbackService;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    private void postJson(String body, org.springframework.test.web.servlet.ResultMatcher expect) throws Exception {
        mockMvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(expect);
    }

    @Test
    void luuFormPhanHoi() throws Exception {
        when(feedbackService.save(any())).thenReturn(new FeedbackSaved(7));

        mockMvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON).content("""
                        {"participantCode":"P07","phase":"before","machine":"gaming",
                         "answers":{"budget_fit":"4","guide":"5","difficulties":"Khó cắm dây nguồn"}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void khongCoCauTraLoiBiTuChoi() throws Exception {
        postJson("{\"participantCode\":\"P07\",\"answers\":{}}", status().isBadRequest());
    }

    @Test
    void maNguoiThamGiaLaBiTuChoi() throws Exception {
        postJson("{\"participantCode\":\"P07; DROP\",\"answers\":{\"guide\":\"4\"}}", status().isBadRequest());
    }

    @Test
    void lanDoLaBiTuChoi() throws Exception {
        postJson("{\"phase\":\"midway\",\"answers\":{\"guide\":\"4\"}}", status().isBadRequest());
    }

    @Test
    void xuatCsvKemTenFile() throws Exception {
        when(feedbackService.exportCsv(eq("k"))).thenReturn("﻿\"id\"\r\n");

        mockMvc.perform(get("/api/feedback/export.csv").param("key", "k"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"pcsmartcare-feedback.csv\""));
    }

    @Test
    void saiKhoaThiKhongXuat() throws Exception {
        when(feedbackService.exportCsv(any())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Sai khoá"));

        mockMvc.perform(get("/api/feedback/export.csv").param("key", "x")).andExpect(status().isForbidden());
    }
}
