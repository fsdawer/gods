package joat.notification;

/** 알림 유형. NotificationEvent.type에 담겨 Kafka로 전달되며 FCM 메시지 본문 생성에 사용된다. */
public enum NotificationType {
    LIKE,    // 내 게시물에 좋아요
    COMMENT, // 내 게시물에 댓글
    FOLLOW   // 누군가 나를 팔로우
}
