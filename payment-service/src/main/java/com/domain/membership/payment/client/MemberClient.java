package com.domain.membership.payment.client;

import com.domain.membership.common.client.MemberSnapshot;
import com.domain.membership.common.exception.BusinessException;
import com.domain.membership.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous client for member-service's internal API.
 * Payment authorization always calls this live — the response is never
 * cached, so billing decisions cannot act on a stale ACTIVE status.
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

    public MemberSnapshot getActiveMember(Long userId) {
        return get("/internal/members/{userId}/active", userId);
    }

    public MemberSnapshot getMember(Long userId) {
        return get("/internal/members/{userId}", userId);
    }

    public MemberSnapshot getMemberById(Long memberId) {
        return get("/internal/members/by-id/{memberId}", memberId);
    }

    private MemberSnapshot get(String uri, Long id) {
        try {
            return restClient.get().uri(uri, id).retrieve().body(MemberSnapshot.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND);
        }
    }
}
