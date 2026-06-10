package joat.joat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context integration smoke test.
 * Requires running PostgreSQL, Redis, and Kafka.
 * Disabled in CI/unit-test runs; enable locally with a real infrastructure stack.
 */
@Disabled("Requires PostgreSQL, Redis, and Kafka — run locally with docker-compose up")
@SpringBootTest
class JoatApplicationTests {

    @Test
    void contextLoads() {
    }

}
