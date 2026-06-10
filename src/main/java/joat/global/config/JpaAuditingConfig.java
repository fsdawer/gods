package joat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정.
 * @EnableJpaAuditing을 통해 BaseEntity의 @CreatedDate, @LastModifiedDate가
 * 자동으로 채워지도록 한다.
 * @SpringBootApplication 클래스에 직접 넣지 않고 별도 Config로 분리하여
 * @WebMvcTest 슬라이스 테스트 시 충돌을 방지한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
