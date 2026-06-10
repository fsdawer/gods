package joat.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연결 및 직렬화 설정.
 * auth:refresh, tags:trending 등 Redis 키에 접근하는 빈들이 이 템플릿을 사용한다.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 키는 String, 값은 JSON으로 직렬화하는 RedisTemplate 빈.
     * JavaTimeModule을 등록하여 LocalDateTime 등 Java 8 날짜 타입이 올바르게 직렬화된다.
     * WRITE_DATES_AS_TIMESTAMPS를 비활성화하여 ISO-8601 문자열 형태로 저장한다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());           // 키: 문자열
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(mapper)); // 값: JSON
        template.setHashKeySerializer(new StringRedisSerializer());       // Hash 키: 문자열
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(mapper)); // Hash 값: JSON
        return template;
    }
}
