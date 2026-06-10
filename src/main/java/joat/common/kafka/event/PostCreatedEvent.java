package joat.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 포스트 생성 시 Kafka "post.created" 토픽으로 발행되는 이벤트 DTO.
 * PostEventProducer가 발행하고 TagEventConsumer가 수신하여 태그 처리에 사용한다.
 * Kafka 직렬화를 위해 기본 생성자(@NoArgsConstructor)가 필요하다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private UUID postId;          // 생성된 포스트의 UUID
    private UUID userId;          // 포스트 작성자의 UUID
    private List<String> tagNames; // 포스트에 첨부된 해시태그 이름 목록
}
