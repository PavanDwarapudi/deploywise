package com.deploywise.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void analyzeReturnsCategory() throws Exception {
        mvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"log\":\"[ERROR] COMPILATION ERROR :\\n[ERROR] cannot find symbol\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryCategory").value("COMPILATION_ERROR"))
                .andExpect(jsonPath("$.findings.length()").value(2));
    }

    @Test
    void emptyLogIsRejected() throws Exception {
        mvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"log\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("log must not be empty"));
    }

    @Test
    void unknownAnalysisIs404() throws Exception {
        mvc.perform(get("/api/analyses/99999")).andExpect(status().isNotFound());
    }

    @Test
    void statsListsEveryCategory() throws Exception {
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.COMPILATION_ERROR").exists())
                .andExpect(jsonPath("$.byCategory.UNKNOWN").exists());
    }
}
