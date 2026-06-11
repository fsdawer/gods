package joat.notification.service;

import joat.notification.dto.NotificationResponse;
import joat.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * NotificationService 구현체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository
            .findTop30ByReceiverIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByReceiverId(userId);
    }
}
