package com.baemin.membership.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "MEMBER_001", "이미 활성화된 멤버십이 존재합니다."),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_002", "멤버십 정보를 찾을 수 없습니다."),

    // Payment
    PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_001", "결제 처리에 실패했습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_002", "결제 정보를 찾을 수 없습니다."),

    // Benefit
    BENEFIT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "BENEFIT_001", "사용 가능한 혜택이 없습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
