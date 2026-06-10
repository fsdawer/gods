package joat.global.config;

import joat.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 토픽 및 컨테이너 팩토리를 등록하는 Config 클래스.
 * 앱 기동 시 Kafka Admin이 정의된 토픽을 자동 생성한다 (이미 있으면 무시).
 *
 * 재시도 정책: 처리 실패 시 1초 간격으로 3회 재시도.
 * 3회 모두 실패하면 DeadLetterPublishingRecoverer가 post.created.dlq 토픽으로 이벤트를 전송한다.
 * TagDlqConsumer가 수신 후 해당 포스트의 tag_status를 FAILED로 마킹하고,
 * TagRetryBatch가 새벽 3시에 FAILED 포스트를 재처리한다.
 */
@Configuration
public class KafkaConfig {

    /**
     * "post.created" 토픽 자동 생성 빈.
     * 파티션 3개(병렬 처리), 복제본 1개(개발 환경 기준).
     * 프로덕션에서는 replicas를 3 이상으로 올려야 한다.
     */
    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.POST_CREATED)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * "post.created.dlq" DLQ 토픽 자동 생성 빈.
     * post.created 처리가 3회 모두 실패한 이벤트가 여기로 온다.
     */
    @Bean
    public NewTopic postCreatedDlqTopic() {
        return TopicBuilder.name(KafkaTopics.POST_CREATED_DLQ)
            .partitions(1)
            .replicas(1)
            .build();
    }

    /**
     * DefaultErrorHandler 빈.
     * 1초 간격 3회 재시도 후 실패하면 DeadLetterPublishingRecoverer로 DLQ 전송.
     * destinationResolver로 DLQ 토픽 이름을 고정 지정한다.
     *
     * @param kafkaTemplate DLQ 발행에 사용할 KafkaTemplate
     */
    @Bean
    public DefaultErrorHandler tagProcessingErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(KafkaTopics.POST_CREATED_DLQ, 0)
        );
        // 1초 간격, 최대 3회 재시도 (총 4회 시도 후 DLQ)
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    /**
     * tag-service 컨슈머 그룹용 KafkaListenerContainerFactory.
     * tagProcessingErrorHandler를 적용하여 재시도 + DLQ 전환이 동작한다.
     *
     * @param consumerFactory Spring Boot 자동 구성된 ConsumerFactory
     * @param errorHandler    위에서 정의한 DefaultErrorHandler
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> tagServiceContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
