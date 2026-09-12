package com.realestate.shared.infrastructure.configuration;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class CoreConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
