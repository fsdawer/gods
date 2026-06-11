package joat.notification;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.NotificationEvent;
import joat.user.entity.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * notification.created 토픽 컨슈머.
 * 이벤트를 수신하면:
 * 1. receiverId로 User 조회 → fcm_token 확인
 * 2. senderId 닉네임 조회 → 알림 메시지 생성
 * 3. FcmService.send()로 푸시 발송 (firebase.enabled=false면 NoopFcmService가 로그만 기록)
 *
 * 자기 자신에게 발생한 이벤트(senderId == receiverId)는 알림 없이 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final FcmService fcmService;
    private final UserRepository userRepository;

    /**
     * notification.created 이벤트 처리.
     * FCM 토큰이 없는 유저는 건너뛴다 (토큰 미등록 또는 로그아웃 상태).
     *
     * @param event 수신된 알림 이벤트
     */
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
        if (receiver.getFcmToken() == null) return; // 토큰 미등록 → 건너뜀

        String senderNickname = userRepository.findById(event.getSenderId())
            .map(User::getNickname)
            .orElse("누군가");

        String title = "JOAT";
        String body = buildMessage(event.getType(), senderNickname);

        fcmService.send(receiver.getFcmToken(), title, body);
        log.debug("[notification] sent type={} to={}", event.getType(), event.getReceiverId());
    }

    /** 알림 유형에 따라 FCM 메시지 본문을 생성한다. */
    private String buildMessage(NotificationType type, String senderNickname) {
        return switch (type) {
            case LIKE    -> senderNickname + "님이 회원님의 게시물을 좋아합니다.";
            case COMMENT -> senderNickname + "님이 회원님의 게시물에 댓글을 남겼습니다.";
            case FOLLOW  -> senderNickname + "님이 회원님을 팔로우하기 시작했습니다.";
        };
    }
}
