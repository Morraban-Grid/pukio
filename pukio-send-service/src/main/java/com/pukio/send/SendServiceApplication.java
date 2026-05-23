package com.pukio.send;

import com.pukio.send.service.FileSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Send Service application entry point.
 * Runs as a standalone executable JAR. (TASK-E1-28)
 * Triggered manually or by a scheduler to transmit local indexed files to Data_Server.
 */
@Slf4j
@SpringBootApplication
public class SendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SendServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(FileSender fileSender) {
        return args -> {
            log.info("=== Pukio Send Service starting ===");
            fileSender.sendFiles();
            log.info("=== Pukio Send Service finished ===");
        };
    }
}
