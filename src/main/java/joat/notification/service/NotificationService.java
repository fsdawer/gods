package joat.notification.service;

import joat.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

/**
 * 알림 목록 조회 및 읽음 처리 서비스 인터페이스.
 */
public interface NotificationService {

    /**
     * 내 알림 목록 조회 (최신 30개).
     *
     * @param userId 조회 대상 유저 ID
     * @return 알림 응답 목록 (최신순)
     */
    List<NotificationResponse> getMyNotifications(UUID userId);

    /**
     * 읽지 않은 알림 수 조회.
     * 홈 화면 벨 배지에 사용한다.
     *
     * @param userId 조회 대상 유저 ID
     * @return 미읽음 알림 수
     */
    long getUnreadCount(UUID userId);

    /**
     * 전체 알림 읽음 처리.
     * 알림 화면 진입 시 호출하여 배지를 초기화한다.
     *
     * @param userId 대상 유저 ID
     */
    void markAllRead(UUID userId);
}
