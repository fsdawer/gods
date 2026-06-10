package joat.common.kafka;

/**
 * Kafka 토픽 이름을 중앙 관리하는 상수 클래스.
 * 프로듀서(PostEventProducer)와 컨슈머(TagEventConsumer)가 동일한 상수를 참조하여
 * 토픽 이름 불일치로 인한 오류를 방지한다.
 */
public final class KafkaTopics {
    private KafkaTopics() {}

    /** 포스트 생성 이벤트 토픽 — TagEventConsumer가 수신하여 태그를 처리한다 */
    public static final String POST_CREATED = "post.created";
}
