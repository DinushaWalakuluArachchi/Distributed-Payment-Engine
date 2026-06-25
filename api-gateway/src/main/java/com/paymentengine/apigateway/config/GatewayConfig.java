package com.paymentengine.apigateway.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {


    private  final RedissonClient redisson;

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public CommandLineRunner seedApiKeys(){
        return args -> {
            RSet<String> validKeys = redisson.getSet("gateway:valid-api-keys");
            validKeys.addAll(Set.of(
                    "test-key-merchant-001",
                    "test-key-merchant-002"
            ));
            log.info("Seeded {} API keys into Redis", validKeys.size());
        };
    }
}
