package com.musicplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Random;

@Configuration
public class RandomConfig {

    /**
     * Production/Development Random bean using standard java.util.Random instance.
     * ThreadLocalRandom.current() is NOT stored in a bean.
     */
    @Bean
    @Profile("!test")
    public Random random() {
        return new Random();
    }

    /**
     * Test Random bean seeded with fixed seed for deterministic reproducible tests.
     */
    @Bean
    @Profile("test")
    public Random seededRandom() {
        return new Random(42L);
    }
}
