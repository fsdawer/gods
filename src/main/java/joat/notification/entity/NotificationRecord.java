package joat.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import joat.notification.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 이력 엔티티.
 * NotificationConsumer가 FCM 발송과 함께 저장하며, GET /api/notifications로 조회된다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID receiverId;

    @Column(nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** LIKE·COMMENT → postId, FOLLOW → null */
    private UUID targetId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static NotificationRecord of(UUID receiverId, UUID senderId, NotificationType type,
                                        UUID targetId, String message) {
        NotificationRecord n = new NotificationRecord();
        n.id = UUID.randomUUID();
        n.receiverId = receiverId;
        n.senderId = senderId;
        n.type = type;
        n.targetId = targetId;
        n.message = message;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    /** 알림을 읽음 처리한다. */
    public void markRead() {
        this.isRead = true;
    }
}
