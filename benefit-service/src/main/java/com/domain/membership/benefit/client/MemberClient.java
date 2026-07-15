package com.domain.membership.benefit.client;

import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Client for member-service's internal API with a short-TTL Redis cache
 * (60s, see RedisConfig). MembershipEventConsumer evicts entries when
 * member-service publishes a state change, so the TTL only bounds staleness
 * if an event is ever missed.
 */
@Component
public class MemberClient {

    private final RestClient restClient;

    // Boot's auto-configured builder carries the SNAKE_CASE ObjectMapper,
    // matching member-service's response naming.
    public MemberClient(RestClient.Builder builder,
                        @Value("${member-service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Cacheable(value = "members", key = "#userId")
    public MemberSnapshot getActiveMember(Long userId) {
        try {
            return restClient.get()
                    .uri("/internal/members/{userId}/active", userId)
                    .retrieve()
                    .body(MemberSnapshot.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND);
        }
    }

    @CacheEvict(value = "members", key = "#userId")
    public void evictMember(Long userId) {
    }
}
