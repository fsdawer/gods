package joat.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 Presigner 빈 설정.
 * S3Service가 Presigned URL을 생성할 때 사용하는 S3Presigner를 등록한다.
 * 인증 정보는 DefaultCredentialsProvider를 통해 환경변수 또는 IAM 역할에서 자동으로 조회된다.
 */
@Configuration
@org.springframework.context.annotation.Profile("!local")
public class S3Config {

    @Value("${aws.s3.region}")
    private String region; // application.yml의 aws.s3.region 값

    /**
     * S3 Presigned URL 생성기 빈.
     * 설정된 리전과 DefaultCredentialsProvider(환경변수/IAM 역할)를 사용한다.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
