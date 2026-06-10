package joat.common.s3;

import joat.common.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * S3 미설정 로컬 개발 환경용 이미지 업로드 컨트롤러.
 * 파일을 서버 로컬 ./uploads/ 폴더에 저장하고 접근 URL을 반환한다.
 * S3 연결 후에는 이 클래스 대신 ImageController(@Profile("!local"))가 활성화된다.
 */
@Profile("local")
@RestController
@RequestMapping("/api/images")
public class LocalImageController {

    private static final String UPLOAD_DIR = "uploads";
    private static final String BASE_URL = "http://localhost:8080/uploads/";

    /**
     * [엔드포인트] POST /api/images/upload — 로컬 파일 업로드
     * multipart/form-data로 이미지를 받아 ./uploads/ 에 저장하고 접근 URL을 반환한다.
     *
     * @param file 업로드할 이미지 파일
     */
    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(
        @AuthenticationPrincipal UUID userId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        // uploads 디렉토리 없으면 자동 생성
        Path uploadDir = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadDir);

        // 파일명 충돌 방지: UUID 접두어 사용
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String savedName = UUID.randomUUID() + "_" + originalName;
        Files.copy(file.getInputStream(), uploadDir.resolve(savedName));

        String imageUrl = BASE_URL + savedName;
        return ApiResponse.ok(new UploadResponse(imageUrl));
    }

    /** 업로드 응답 DTO */
    @Getter
    @AllArgsConstructor
    public static class UploadResponse {
        /** 업로드된 이미지에 접근할 수 있는 URL */
        private final String imageUrl;
    }
}
