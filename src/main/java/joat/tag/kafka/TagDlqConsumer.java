package joat.tag.kafka;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.PostCreatedEvent;
import joat.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * post.created.dlq 토픽 컨슈머.
 * TagEventConsumer가 3회 재시도 후에도 실패한 이벤트가 이 토픽으로 도달한다.
 * 해당 포스트의 tag_status를 FAILED로 마킹하고 pending_tag_names를 저장한다.
 * TagRetryBatch가 새벽 3시에 FAILED 포스트를 읽어 processTags를 재시도한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TagDlqConsumer {

    private final PostService postService;

    /**
     * DLQ 이벤트 수신 핸들러.
     * 포스트의 tag_status를 FAILED로 전환하고 pending_tag_names를 저장한다.
     * 이후 TagRetryBatch가 이 정보를 사용해 재처리를 시도한다.
     *
     * @param event 3회 재시도 후 최종 실패한 PostCreatedEvent (tagNames 포함)
     */
    @KafkaListener(topics = KafkaTopics.POST_CREATED_DLQ, groupId = "tag-service-dlq")
    public void onDeadLetter(PostCreatedEvent event) {
        log.error("[DLQ] Tag processing permanently failed. postId={}, tags={}",
            event.getPostId(), event.getTagNames());
        postService.markTagFailed(event.getPostId(), event.getTagNames());
    }
}
