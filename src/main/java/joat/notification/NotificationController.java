package joat.notification;

import joat.common.response.ApiResponse;
import joat.notification.dto.NotificationResponse;
import joat.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 알림 관련 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * [엔드포인트] GET /api/notifications — 내 알림 목록 조회 (최신 30개)
     * 앱 알림 화면 진입 시 호출한다.
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@AuthenticationPrincipal UUID userId) {
        List<NotificationResponse> result = notificationRepository
            .findTop30ByReceiverIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
        return ApiResponse.ok(result);
    }

    /**
     * [엔드포인트] GET /api/notifications/unread-count — 안 읽은 알림 수
     * 홈 화면 벨 아이콘 배지에 사용한다.
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(notificationRepository.countByReceiverIdAndIsReadFalse(userId));
    }

    /**
     * [엔드포인트] POST /api/notifications/read-all — 전체 읽음 처리
     * 알림 화면 진입 시 호출하여 배지를 초기화한다.
     */
    @PostMapping("/read-all")
    @Transactional
    public ApiResponse<Void> readAll(@AuthenticationPrincipal UUID userId) {
        notificationRepository.markAllReadByReceiverId(userId);
        return ApiResponse.ok();
    }
}
