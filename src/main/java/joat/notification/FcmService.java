package joat.notification;

/**
 * FCM 푸시 알림 발송 서비스 인터페이스.
 * firebase.enabled=false 환경에서는 NoopFcmService가 주입된다.
 */
public interface FcmService {

    /**
     * 디바이스 FCM 토큰으로 푸시 알림을 발송한다.
     *
     * @param fcmToken 수신 디바이스 토큰 (users.fcm_token)
     * @param title    알림 제목
     * @param body     알림 본문
     */
    void send(String fcmToken, String title, String body);
}
