package joat.notification;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 이벤트를 Kafka notification.created 토픽으로 발행하는 프로듀서.
 * spring.kafka.enabled=false 이면 빈이 생성되지 않아 Kafka 없는 환경에서도 동작한다.
 * PostServiceImpl·CommentServiceImpl·UserServiceImpl에서 Optional로 주입받아 조건부로 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 알림 이벤트를 notification.created 토픽으로 비동기 발행.
     * receiverId를 파티션 키로 사용하여 같은 유저의 알림이 순서대로 처리된다.
     * 발행 실패 시 로그만 남기고 예외를 전파하지 않는다 — 알림 실패가 본 API 응답에 영향을 주면 안 됨.
     *
     * @param event 발행할 알림 이벤트
     */
    public void publish(NotificationEvent event) {
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_CREATED, event.getReceiverId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[notification] publish failed type={} sender={} receiver={}: {}",
                        event.getType(), event.getSenderId(), event.getReceiverId(), ex.getMessage());
                }
            });
    }
}
