package com.washer.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;
import com.washer.backend.cloudbase.CloudBasePgProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Profile("cloudbase")
@Configuration
@EnableConfigurationProperties(CloudBasePgProperties.class)
public class CloudBaseHttpConfiguration {

    @Bean
    RestClient.Builder cloudBaseRestClientBuilder() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    CloudBasePgClient cloudBasePgClient(
        RestClient.Builder cloudBaseRestClientBuilder,
        ObjectMapper objectMapper,
        CloudBasePgProperties properties
    ) {
        return new CloudBasePgClient(cloudBaseRestClientBuilder, objectMapper, properties);
    }
}
