package com.xypu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.xypu"})
public class MediumwebRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediumwebRunApplication.class, args);
    }
}
