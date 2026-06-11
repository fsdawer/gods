package joat.notification.dto;

import joat.notification.NotificationType;
import joat.notification.entity.NotificationRecord;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class NotificationResponse {

    private final UUID id;
    private final NotificationType type;
    private final UUID senderId;
    private final UUID targetId;
    private final String message;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    private NotificationResponse(NotificationRecord n) {
        this.id = n.getId();
        this.type = n.getType();
        this.senderId = n.getSenderId();
        this.targetId = n.getTargetId();
        this.message = n.getMessage();
        this.isRead = n.isRead();
        this.createdAt = n.getCreatedAt();
    }

    public static NotificationResponse from(NotificationRecord n) {
        return new NotificationResponse(n);
    }
}
