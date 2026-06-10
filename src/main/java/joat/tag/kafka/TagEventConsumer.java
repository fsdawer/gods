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
 * groupId="tag-service" 로 독립적인 컨슈머 그룹을 유지한다.
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
     * 처리 실패 시 로그만 남기고 예외를 전파하지 않아 메시지 처리가 중단되지 않는다.
     *
     * @param event PostCreatedEvent (postId, userId, tagNames)
     */
    @KafkaListener(topics = KafkaTopics.POST_CREATED, groupId = "tag-service")
    public void onPostCreated(PostCreatedEvent event) {
        List<String> tagNames = event.getTagNames();
        if (tagNames == null || tagNames.isEmpty()) return; // 태그 없는 포스트는 처리 생략
        try {
            tagService.processTags(event.getPostId(), tagNames);
        } catch (Exception e) {
            log.error("Tag processing failed for postId={}: {}", event.getPostId(), e.getMessage());
        }
    }
}
