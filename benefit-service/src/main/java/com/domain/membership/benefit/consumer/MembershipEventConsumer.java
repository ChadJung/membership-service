package com.domain.membership.benefit.consumer;

import com.domain.membership.benefit.client.MemberClient;
import com.domain.membership.common.event.MembershipEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven invalidation of the member snapshot cache: member-service
 * owns member state and publishes every change, so this consumer keeps the
 * local cache consistent without benefit-service polling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventConsumer {

    private final MemberClient memberClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "membership-events", groupId = "benefit-service")
    public void handleMembershipEvent(String payload) {
        MembershipEvent event;
        try {
            event = objectMapper.readValue(payload, MembershipEvent.class);
        } catch (JsonProcessingException e) {
            log.error("멤버십 이벤트 파싱 실패: payload={}", payload, e);
            return;
        }

        memberClient.evictMember(event.userId());
        log.info("멤버십 이벤트 수신, 회원 캐시 무효화: type={}, userId={}", event.type(), event.userId());
    }
}
