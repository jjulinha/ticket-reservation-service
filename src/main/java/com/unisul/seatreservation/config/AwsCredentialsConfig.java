package com.unisul.seatreservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Configuration
public class AwsCredentialsConfig {

    @Value("${custom.aws.access-key}")
    private String accessKey;

    @Value("${custom.aws.secret-key}")
    private String secretKey;

    @Value("${custom.aws.session-token}")
    private String sessionToken;

    @Bean
    @Primary
    public AwsCredentialsProvider customAwsCredentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                        accessKey,
                        secretKey,
                        sessionToken
                )
        );
    }
}