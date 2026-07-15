package com.domain.membership.member.event;

import com.domain.membership.common.event.MembershipEvent;
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
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("membership-events", payload);
            log.info("이벤트 발행: topic=membership-events, payload={}", payload);
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패", e);
        }
    }
}
