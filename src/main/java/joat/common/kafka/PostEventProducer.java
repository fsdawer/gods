package joat.common.kafka;

import joat.common.kafka.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 포스트 생성 이벤트를 Kafka로 발행하는 프로듀서.
 * spring.kafka.enabled=false 이면 빈 자체가 생성되지 않아 Kafka 없는 환경에서도 앱이 동작한다.
 * PostServiceImpl에서 Optional<PostEventProducer>로 주입받아 조건부로 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class PostEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 포스트 생성 이벤트를 "post.created" 토픽으로 비동기 발행.
     * 발행 실패 시 로그만 남기고 예외를 전파하지 않아 포스트 저장에는 영향을 주지 않는다.
     * TagEventConsumer(groupId="tag-service")가 이 이벤트를 수신하여 태그를 처리한다.
     *
     * @param event 발행할 포스트 생성 이벤트 (postId, userId, tagNames 포함)
     */
    public void publishPostCreated(PostCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.POST_CREATED, event.getPostId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) log.error("Failed to publish PostCreatedEvent: {}", ex.getMessage());
            });
    }
}
