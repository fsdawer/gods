package joat.notification.controller;

import joat.common.response.ApiResponse;
import joat.notification.dto.NotificationResponse;
import joat.notification.service.NotificationService;
import joat.notification.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * 알림 관련 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    /**
     * [엔드포인트] GET /api/notifications/stream — SSE 실시간 알림 스트림
     * 클라이언트는 이 연결을 유지하다가 "notification" 이벤트를 수신하면
     * 알림 목록과 배지를 갱신한다. 연결이 끊기면 클라이언트에서 재연결을 시도한다.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UUID userId) {
        return sseNotificationService.connect(userId);
    }

    /**
     * [엔드포인트] GET /api/notifications — 내 알림 목록 조회 (최신 30개)
     * 앱 알림 화면 진입 시 및 SSE 재연결 후 놓친 알림 동기화에 사용한다.
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(notificationService.getMyNotifications(userId));
    }

    /**
     * [엔드포인트] GET /api/notifications/unread-count — 안 읽은 알림 수
     * 초기 앱 진입 시 배지 초기화에 사용한다. 이후 SSE 이벤트로 갱신.
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * [엔드포인트] POST /api/notifications/read-all — 전체 읽음 처리
     * 알림 화면 진입 시 호출하여 배지를 초기화한다.
     */
    @PostMapping("/read-all")
    public ApiResponse<Void> readAll(@AuthenticationPrincipal UUID userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.ok();
    }
}
