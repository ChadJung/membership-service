package com.domain.membership.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// scanBasePackages includes com.domain.membership.common (GlobalExceptionHandler).
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.domain.membership")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
