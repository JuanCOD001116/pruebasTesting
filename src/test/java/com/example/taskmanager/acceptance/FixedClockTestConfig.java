package com.example.taskmanager.acceptance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@TestConfiguration
class FixedClockTestConfig {

    static final ZoneId ZONE = ZoneId.of("America/Bogota");
    static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Bean
    @Primary
    Clock acceptanceClock() {
        return Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    }
}
