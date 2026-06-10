package joat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 설정.
 * - 엔드포인트: /ws (SockJS 폴백 지원)
 * - 구독: /topic/team/{teamId} — 팀방별 채팅 메시지 브로드캐스트
 * - 발신: /app/team/{teamId}/message — 클라이언트 → 서버 메시지 전송
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정.
     * /topic 접두사는 인-메모리 단순 브로커가 처리하고,
     * /app 접두사는 @MessageMapping 핸들러로 라우팅된다.
     *
     * @param config 브로커 설정 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP WebSocket 엔드포인트 등록.
     * /ws 경로로 연결하며 SockJS 폴백을 지원한다.
     *
     * @param registry 엔드포인트 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹 클라이언트용 — SockJS 폴백 지원
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // React Native 앱 전용 — 순수 WebSocket (SockJS 없음, TextDecoder 불필요)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}
