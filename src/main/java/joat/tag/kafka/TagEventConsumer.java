package joat.tag.kafka;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.PostCreatedEvent;
import joat.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 포스트 생성 이벤트를 Kafka에서 수신하여 태그를 처리하는 컨슈머.
 * spring.kafka.enabled=false 이면 빈 자체가 생성되지 않아 Kafka 없는 환경에서도 동작한다.
 * groupId="tag-service", containerFactory="tagServiceContainerFactory" 지정.
 *
 * 예외 전파 정책: 처리 실패 시 예외를 그대로 던져 KafkaConfig에 등록된
 * DefaultErrorHandler(1초 간격 3회 재시도 → DLQ)가 동작하도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TagEventConsumer {

    private final TagService tagService;

    /**
     * "post.created" 토픽 메시지 수신 핸들러.
     * 태그명 목록이 있을 때만 TagService.processTags를 호출한다.
     * 예외는 전파하여 DefaultErrorHandler의 재시도 → DLQ 전환이 동작하게 한다.
     *
     * @param event PostCreatedEvent (postId, userId, tagNames)
     */
    @KafkaListener(
        topics = KafkaTopics.POST_CREATED,
        groupId = "tag-service",
        containerFactory = "tagServiceContainerFactory"
    )
    public void onPostCreated(PostCreatedEvent event) {
        List<String> tagNames = event.getTagNames();
        if (tagNames == null || tagNames.isEmpty()) return;
        tagService.processTags(event.getPostId(), tagNames); // 실패 시 예외 전파 → retry → DLQ
    }
}
