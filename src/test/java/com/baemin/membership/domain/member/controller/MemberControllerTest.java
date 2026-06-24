package com.baemin.membership.domain.member.controller;

import com.baemin.membership.domain.member.dto.MemberSubscribeRequest;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@EmbeddedKafka(partitions = 1, topics = {"membership-events", "payment-events"})
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/memberships - 멤버십 가입 API")
    void subscribe_api() throws Exception {
        MemberSubscribeRequest request = new MemberSubscribeRequest(100L, MembershipGrade.BASIC);

        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(100))
                .andExpect(jsonPath("$.data.grade").value("BASIC"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/memberships/{userId} - 멤버십 조회 API")
    void getMembership_api() throws Exception {
        // Subscribe first
        MemberSubscribeRequest request = new MemberSubscribeRequest(200L, MembershipGrade.PREMIUM);
        mockMvc.perform(post("/api/v1/memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then query
        mockMvc.perform(get("/api/v1/memberships/200"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade_display_name").value("프리미엄"));
    }

    @Test
    @DisplayName("DELETE /api/v1/memberships/{userId} - 멤버십 해지 API")
    void cancel_api() throws Exception {
        // Subscribe first
        MemberSubscribeRequest request = new MemberSubscribeRequest(300L, MembershipGrade.BASIC);
        mockMvc.perform(post("/api/v1/memberships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then cancel
        mockMvc.perform(delete("/api/v1/memberships/300"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
