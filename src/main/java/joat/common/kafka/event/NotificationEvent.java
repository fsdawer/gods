package joat.common.kafka.event;

import joat.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 알림 발생 이벤트 — notification.created 토픽으로 발행된다.
 * NotificationConsumer가 수신하여 receiverId의 FCM 토큰을 조회하고 푸시를 발송한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    /** 알림 유형 (LIKE / COMMENT / FOLLOW) */
    private NotificationType type;

    /** 알림을 유발한 유저 UUID */
    private UUID senderId;

    /** 알림을 받을 유저 UUID */
    private UUID receiverId;

    /**
     * 알림 대상 리소스 UUID.
     * LIKE·COMMENT → postId, FOLLOW → null
     */
    private UUID targetId;
}
