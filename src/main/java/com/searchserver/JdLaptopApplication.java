package com.searchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JdLaptopApplication {
    public static void main(String[] args) {
        SpringApplication.run(JdLaptopApplication.class, args);
    }
}
