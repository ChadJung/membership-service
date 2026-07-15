package com.domain.membership.member.consumer;

import com.domain.membership.common.event.PaymentEvent;
import com.domain.membership.member.repository.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renews the membership when payment-service reports a completed payment.
 * This is the only path that extends expiredAt — member-service owns the
 * member state and payment-service never writes to member_db directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "member-service")
    @Transactional
    public void handlePaymentEvent(String payload) {
        PaymentEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentEvent.class);
        } catch (JsonProcessingException e) {
            log.error("결제 이벤트 파싱 실패: payload={}", payload, e);
            return;
        }

        if (!PaymentEvent.TYPE_COMPLETED.equals(event.type())) {
            return;
        }

        memberRepository.findById(event.memberId()).ifPresentOrElse(
                member -> {
                    member.renew();
                    log.info("결제 완료 이벤트 수신, 멤버십 갱신: memberId={}, userId={}",
                            event.memberId(), event.userId());
                },
                () -> log.warn("결제 이벤트에 해당하는 회원 없음: memberId={}", event.memberId())
        );
    }
}
