package com.washer.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!cloudbase")
@Configuration
@MapperScan("com.washer.backend.mapper")
public class LocalMybatisConfiguration {
}

