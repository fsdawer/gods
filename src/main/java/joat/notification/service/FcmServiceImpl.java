package joat.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Firebase Admin SDK를 사용한 FCM 푸시 발송 구현체.
 * firebase.enabled=true 일 때만 생성된다. 비활성화 시 NoopFcmService가 대신 주입된다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmServiceImpl implements FcmService {

    /**
     * FCM 단건 푸시 발송.
     * 발송 실패 시 예외를 로그로만 기록하고 전파하지 않는다 — 알림 실패가 Kafka 재시도를 유발하면 안 됨.
     *
     * @param fcmToken 수신 디바이스 토큰
     * @param title    알림 제목
     * @param body     알림 본문
     */
    @Override
    public void send(String fcmToken, String title, String body) {
        Message message = Message.builder()
            .setToken(fcmToken)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .build();
        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("[fcm] sent messageId={}", messageId);
        } catch (Exception e) {
            log.error("[fcm] send failed token={}...: {}", fcmToken.substring(0, 10), e.getMessage());
        }
    }
}
