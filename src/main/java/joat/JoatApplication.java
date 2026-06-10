package joat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JoatApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoatApplication.class, args);
    }
}