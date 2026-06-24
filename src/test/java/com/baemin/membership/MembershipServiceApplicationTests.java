package com.baemin.membership;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@EmbeddedKafka(partitions = 1, topics = {"membership-events", "payment-events"})
class MembershipServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
