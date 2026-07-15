package com.domain.membership.benefit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages includes com.domain.membership.common (GlobalExceptionHandler).
@SpringBootApplication(scanBasePackages = "com.domain.membership")
public class BenefitServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenefitServiceApplication.class, args);
    }
}
