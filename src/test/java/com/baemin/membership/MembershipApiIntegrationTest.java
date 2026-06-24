package com.baemin.membership;

import com.baemin.membership.domain.member.dto.MemberSubscribeRequest;
import com.baemin.membership.domain.member.entity.MembershipGrade;
import com.baemin.membership.domain.payment.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 컨트롤러 → 서비스 → 리포지토리(H2) 전 구간을 MockMvc로 검증하는 통합 테스트.
 * Kafka 발행은 {@link KafkaTemplate}을 모킹하여 외부 브로커 없이 동작한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MembershipApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private void subscribe(long userId, MembershipGrade grade) throws Exception {
        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberSubscribeRequest(userId, grade))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("가입 → 결제 → 결제내역 조회까지 전 구간이 동작한다")
    void subscribe_then_pay_then_history() throws Exception {
        // 가입 (PREMIUM)
        subscribe(1001L, MembershipGrade.PREMIUM);

        // 결제 — 금액은 등급 월 요금(7900)으로 결정
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(1001L, "CARD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(MembershipGrade.PREMIUM.getMonthlyFee()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.transaction_id").isNotEmpty());

        // 결제 내역
        mockMvc.perform(get("/api/v1/payments/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].amount").value(MembershipGrade.PREMIUM.getMonthlyFee()));
    }

    @Test
    @DisplayName("이미 활성 멤버십이 있으면 409(MEMBER_001)로 가입 거부")
    void duplicate_subscribe_returns_409() throws Exception {
        subscribe(1002L, MembershipGrade.BASIC);

        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberSubscribeRequest(1002L, MembershipGrade.BASIC))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_001"));
    }

    @Test
    @DisplayName("필수값 누락 시 400(COMMON_001) 검증 에러")
    void missing_required_field_returns_400() throws Exception {
        // user_id 누락
        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"BASIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("활성 멤버십 없이 결제 시 404(MEMBER_002)")
    void payment_without_membership_returns_404() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(9999L, "CARD"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_002"));
    }

    @Test
    @DisplayName("Kafka 이벤트 발행이 실패해도 가입 요청은 성공한다 (@Async 격리)")
    void subscribe_succeeds_even_if_kafka_publish_fails() throws Exception {
        // 이벤트 발행을 동기 호출하면 요청 스레드에서 예외가 전파되어 500이 되지만,
        // @EnableAsync로 리스너가 별도 스레드에서 실행되므로 가입은 정상 완료된다.
        given(kafkaTemplate.send(anyString(), anyString()))
                .willThrow(new RuntimeException("broker down"));

        mockMvc.perform(post("/api/v1/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberSubscribeRequest(1003L, MembershipGrade.BASIC))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
