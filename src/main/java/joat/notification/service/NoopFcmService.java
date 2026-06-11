package joat.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * firebase.enabled=false(기본값) 환경에서 FcmService 자리를 채우는 no-op 구현체.
 * Firebase 자격증명 없이도 앱이 시작되고, 알림은 로그로만 기록된다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class NoopFcmService implements FcmService {

    @Override
    public void send(String fcmToken, String title, String body) {
        log.info("[fcm-noop] title='{}' body='{}' (firebase.enabled=false)", title, body);
    }
}
