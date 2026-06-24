package com.baemin.membership.global.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void handleMembershipEvent(MembershipEvent event) {
        publishEvent("membership-events", event);
    }

    @Async
    @EventListener
    public void handlePaymentEvent(PaymentEvent event) {
        publishEvent("payment-events", event);
    }

    private void publishEvent(String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload);
            log.info("이벤트 발행: topic={}, payload={}", topic, payload);
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패", e);
        }
    }
}
