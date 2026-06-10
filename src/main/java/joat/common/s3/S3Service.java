package joat.common.s3;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * S3 Presigned URL 생성 서비스.
 * 이미지 업로드 시 서버를 경유하지 않고 앱이 S3에 직접 PUT할 수 있도록
 * 서명된 업로드 URL과 최종 접근 URL을 생성한다.
 */
@Service
@org.springframework.context.annotation.Profile("!local")
public class S3Service {

    private final S3Presigner presigner;
    private final String bucket;       // S3 버킷 이름 (aws.s3.bucket)
    private final String region;       // AWS 리전 (aws.s3.region)
    private final long expirySeconds;  // Presigned URL 유효 시간 초 (aws.s3.presigned-expiry)

    public S3Service(
        S3Presigner presigner,
        @Value("${aws.s3.bucket}") String bucket,
        @Value("${aws.s3.region}") String region,
        @Value("${aws.s3.presigned-expiry}") long expirySeconds
    ) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.region = region;
        this.expirySeconds = expirySeconds;
    }

    /**
     * S3 PUT용 Presigned URL과 업로드 완료 후 접근할 파일 URL을 생성한다.
     * S3 키는 "{userId}/{randomUUID}/{fileName}" 구조로 유저별로 분리 저장된다.
     *
     * @param fileName    업로드할 파일 이름
     * @param contentType MIME 타입 (예: image/jpeg)
     * @param userId      업로드 요청자 UUID (S3 키 경로에 포함)
     * @return presignedUrl(PUT 요청용 서명 URL) + fileUrl(업로드 완료 후 접근 URL)
     */
    public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType, UUID userId) {
        // S3 키: {userId}/{randomUUID}/{fileName} 형태로 경로 충돌 방지
        String key = userId + "/" + UUID.randomUUID() + "/" + fileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expirySeconds))
            .putObjectRequest(objectRequest)
            .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        // 업로드 완료 후 앱이 저장할 최종 공개 접근 URL
        String fileUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

        return new PresignedUrlResponse(presigned.url().toString(), fileUrl);
    }

    /**
     * Presigned URL 응답 DTO.
     */
    @Getter
    @AllArgsConstructor
    public static class PresignedUrlResponse {
        private final String presignedUrl; // 앱이 이미지를 PUT할 서명된 S3 URL
        private final String fileUrl;      // 업로드 완료 후 저장/표시할 최종 이미지 URL
    }
}
