package com.baemin.membership.global.scheduler;

import com.baemin.membership.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentService paymentService;

    @Scheduled(cron = "0 0 6 * * *")
    public void processScheduledRenewals() {
        log.info("정기결제 스케줄러 실행");
        paymentService.processScheduledRenewals();
    }
}
