package joat.notification;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.NotificationEvent;
import joat.notification.entity.NotificationRecord;
import joat.notification.repository.NotificationRepository;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * notification.created 토픽 컨슈머.
 * 이벤트를 수신하면:
 * 1. 자기 자신 이벤트 필터링
 * 2. 알림 메시지 생성 후 notifications 테이블에 저장 (항상)
 * 3. FCM 토큰이 있는 경우에만 FCM 푸시 발송
 *
 * DB 저장과 FCM 발송을 분리하여 FCM 토큰 없는 유저도 앱 내 알림 목록에서 확인 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * notification.created 이벤트 처리.
     * DB 저장은 항상 수행, FCM은 토큰 있을 때만 발송.
     *
     * @param event 수신된 알림 이벤트
     */
    @Transactional
    @KafkaListener(topics = KafkaTopics.NOTIFICATION_CREATED, groupId = "notification-service")
    public void handle(NotificationEvent event) {
        // 자기 자신에게 알림 발송하지 않음 (본인 게시물에 본인이 좋아요 등)
        if (event.getSenderId().equals(event.getReceiverId())) return;

        Optional<User> receiverOpt = userRepository.findById(event.getReceiverId());
        if (receiverOpt.isEmpty()) {
            log.warn("[notification] receiver not found: {}", event.getReceiverId());
            return;
        }
        User receiver = receiverOpt.get();

        String senderNickname = userRepository.findById(event.getSenderId())
            .map(User::getNickname)
            .orElse("누군가");
        String message = buildMessage(event.getType(), senderNickname);

        // DB 저장 — FCM 토큰 유무와 무관하게 항상 기록 (앱 내 알림 목록용)
        notificationRepository.save(
            NotificationRecord.of(event.getReceiverId(), event.getSenderId(),
                event.getType(), event.getTargetId(), message)
        );

        // FCM 푸시 — 토큰이 있을 때만 발송
        if (receiver.getFcmToken() != null) {
            fcmService.send(receiver.getFcmToken(), "JOAT", message);
        }
        log.debug("[notification] saved type={} to={}", event.getType(), event.getReceiverId());
    }

    /** 알림 유형에 따라 메시지 본문을 생성한다. */
    private String buildMessage(NotificationType type, String senderNickname) {
        return switch (type) {
            case LIKE    -> senderNickname + "님이 회원님의 게시물을 좋아합니다.";
            case COMMENT -> senderNickname + "님이 회원님의 게시물에 댓글을 남겼습니다.";
            case FOLLOW  -> senderNickname + "님이 회원님을 팔로우하기 시작했습니다.";
        };
    }
}
