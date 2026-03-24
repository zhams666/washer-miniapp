package com.washer.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.washer.backend.mapper")
public class WasherBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasherBackendApplication.class, args);
    }
}
