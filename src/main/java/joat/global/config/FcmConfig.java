package joat.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Admin SDK 초기화 설정.
 * firebase.enabled=true 이고 service-account-key 파일이 존재할 때만 활성화된다.
 * 로컬 개발 환경에서는 firebase.enabled=false(기본값)로 NoopFcmService가 대신 동작한다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmConfig {

    /** Firebase 서비스 계정 키 JSON 파일 경로 (application.yaml에 설정) */
    @Value("${firebase.service-account-key}")
    private String serviceAccountKeyPath;

    /**
     * Firebase 앱을 초기화한다.
     * 이미 초기화된 경우(기존 FirebaseApp 존재) 중복 초기화를 건너뛴다.
     */
    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            FirebaseApp.initializeApp(options);
            log.info("[firebase] initialized with key: {}", serviceAccountKeyPath);
        }
    }
}
