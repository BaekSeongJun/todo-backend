package com.example.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * app.storage-type=s3일 때만 활성화되는 AWS SDK 빈 구성. 자격증명은 SDK 기본 체인(환경변수·
 * 프로파일·IMDS 등 자동 탐색)을 쓰지 않고 AWS_ACCESS_KEY_ID·AWS_SECRET_ACCESS_KEY 두
 * 환경변수만 Spring이 명시적으로 읽어 StaticCredentialsProvider로 주입한다.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage-type", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${app.s3-region}") String region,
            @Value("${aws.access-key-id}") String accessKeyId,
            @Value("${aws.secret-access-key}") String secretAccessKey) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${app.s3-region}") String region,
            @Value("${aws.access-key-id}") String accessKeyId,
            @Value("${aws.secret-access-key}") String secretAccessKey) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }
}
