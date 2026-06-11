package joat.notification.service;

import joat.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리 및 실시간 알림 발송 서비스.
 * 연결 중인 유저만 Map에 유지하며, 연결이 끊기면 자동으로 제거된다.
 * 서버가 단일 인스턴스인 경우에 적합. 멀티 인스턴스 환경에서는 Redis Pub/Sub을 통해 연동 필요.
 */
@Slf4j
@Service
public class SseNotificationService {

    /** timeout 30분 — 그 이상 idle이면 클라이언트가 재연결하도록 유도 */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    /** 현재 연결 중인 유저의 SseEmitter. 유저가 오프라인이면 항목 없음 */
    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트의 SSE 연결 요청을 처리하고 SseEmitter를 반환한다.
     * 이미 연결이 있으면 기존 연결을 닫고 새 연결로 교체한다 (탭 재진입, 재연결 시).
     *
     * @param userId 연결 요청 유저 ID
     * @return 설정이 완료된 SseEmitter (컨트롤러가 반환하면 Spring이 스트림을 유지)
     */
    public SseEmitter connect(UUID userId) {
        // 기존 연결이 있으면 닫고 교체 (중복 연결 방지)
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) existing.complete();

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.put(userId, emitter);

        // 연결 종료 시 Map에서 제거 — 세 경우 모두 처리
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        // 연결 직후 ping 이벤트 전송 — 일부 프록시/방화벽이 첫 데이터 없으면 연결을 끊음
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        log.debug("[sse] connected userId={} total={}", userId, emitters.size());
        return emitter;
    }

    /**
     * 특정 유저에게 실시간 알림을 발송한다.
     * 유저가 오프라인(연결 없음)이면 아무 동작도 하지 않는다 — DB에 저장되므로 나중에 조회 가능.
     *
     * @param userId   수신 유저 ID
     * @param response 전송할 알림 데이터
     */
    public void send(UUID userId, NotificationResponse response) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return; // 오프라인 유저 — DB에 저장되어 있으므로 무시

        try {
            emitter.send(SseEmitter.event().name("notification").data(response));
            log.debug("[sse] sent type={} to={}", response.getType(), userId);
        } catch (IOException e) {
            // 발송 중 연결이 끊긴 경우 — 조용히 제거
            emitters.remove(userId);
            log.debug("[sse] send failed (disconnected) userId={}", userId);
        }
    }
}
