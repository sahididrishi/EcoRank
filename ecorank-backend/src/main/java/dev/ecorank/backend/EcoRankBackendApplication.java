package dev.ecorank.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import dev.ecorank.backend.config.EcoRankProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EcoRankProperties.class)
public class EcoRankBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoRankBackendApplication.class, args);
    }
}
