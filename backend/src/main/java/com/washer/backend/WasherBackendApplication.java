package com.washer.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.washer.backend.config.CommerceProperties;
import com.washer.backend.config.DeviceGatewayProperties;
import com.washer.backend.config.PointMallProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.washer.backend.mapper")
@EnableScheduling
@EnableConfigurationProperties({DeviceGatewayProperties.class, CommerceProperties.class, PointMallProperties.class})
public class WasherBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasherBackendApplication.class, args);
    }
}
