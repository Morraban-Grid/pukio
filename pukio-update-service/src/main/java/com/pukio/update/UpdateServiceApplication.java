package com.pukio.update;

import com.pukio.update.service.FileReceiver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Update Service application entry point.
 * Starts the TCP server as a standalone process. (TASK-E1-36)
 */
@Slf4j
@SpringBootApplication
public class UpdateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpdateServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner startServer(FileReceiver fileReceiver) {
        return args -> {
            log.info("=== Pukio Update Service starting ===");
            fileReceiver.start();
        };
    }
}
