package joat.common.s3;

import joat.common.response.ApiResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 이미지 업로드용 S3 Presigned URL 발급 컨트롤러.
 * 앱은 이 API로 서명된 URL을 받아 서버를 경유하지 않고 S3에 직접 PUT 요청을 보낸다.
 */
@org.springframework.context.annotation.Profile("!local")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    /** Presigned URL 발급 요청 DTO */
    @Getter
    @NoArgsConstructor
    static class PresignedUrlRequest {
        /** 업로드할 파일명 (확장자 포함, 예: photo.jpg) */
        private String fileName;
        /** MIME 타입 (예: image/jpeg) */
        private String contentType;
    }

    /**
     * [엔드포인트] POST /api/images/presigned-url — S3 직접 업로드용 Presigned URL 발급
     * 앱이 이미지를 올리기 전에 먼저 이 API를 호출하여 서명된 PUT URL을 받는다.
     * 응답의 presignedUrl로 S3에 직접 PUT, fileUrl을 포스트/프로필 이미지 URL로 저장한다.
     *
     * @param req fileName(파일명), contentType(MIME 타입)
     */
    @PostMapping("/presigned-url")
    public ApiResponse<S3Service.PresignedUrlResponse> presignedUrl(
        @AuthenticationPrincipal UUID userId,
        @RequestBody PresignedUrlRequest req
    ) {
        return ApiResponse.ok(s3Service.generatePresignedUrl(req.getFileName(), req.getContentType(), userId));
    }
}
