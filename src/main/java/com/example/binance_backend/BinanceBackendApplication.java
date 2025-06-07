package com.example.binance_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // habilita a execução periódica dos métodos @Scheduled
public class BinanceBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BinanceBackendApplication.class, args);
    }
}
