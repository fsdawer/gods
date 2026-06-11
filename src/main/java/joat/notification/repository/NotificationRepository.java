package joat.notification.repository;

import joat.notification.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {

    /**
     * 수신자 기준 최신순 30개 조회 — 알림 목록 화면에 사용.
     * idx_notifications_receiver 인덱스로 커버됨.
     */
    List<NotificationRecord> findTop30ByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    /** 읽지 않은 알림 수 — 앱 아이콘 배지 표시에 사용. */
    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    /** 수신자의 모든 안 읽은 알림을 일괄 읽음 처리. */
    @Modifying
    @Query("UPDATE NotificationRecord n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false")
    void markAllReadByReceiverId(UUID receiverId);
}
