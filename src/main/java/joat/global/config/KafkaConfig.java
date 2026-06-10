package joat.global.config;

import joat.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 토픽 및 설정 빈을 등록하는 Config 클래스.
 * 앱 기동 시 Kafka Admin이 정의된 토픽을 자동 생성한다 (이미 있으면 무시).
 */
@Configuration
public class KafkaConfig {

    /**
     * "post.created" 토픽 자동 생성 빈.
     * 파티션 3개(병렬 처리), 복제본 1개(개발 환경 기준)로 설정된다.
     * 프로덕션에서는 복제본 수를 늘려야 한다.
     */
    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.POST_CREATED)
            .partitions(3)
            .replicas(1)

            .build();
    }
}
