# 구조적 리팩터링 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 설정 파일 global 패키지 통합, record 제거, domain→entity 패키지 rename, TodoItemResponse 분리, .claude docs/skills 하네스 구축

**Architecture:** 순수 구조 리팩터링 — 비즈니스 로직 변경 없음. Config 파일들은 `joat.global.config`로 집중, JPA 엔티티 패키지는 `domain`→`entity` rename, record 클래스는 Lombok `@Getter+@AllArgsConstructor`로 대체, .claude 디렉토리에 에이전트별 docs/skills 하네스 추가

**Tech Stack:** Java 17, Spring Boot 3.4.5, Lombok, JPA @IdClass

---

## 파일 맵

| 작업 | 생성 | 삭제/수정 |
|---|---|---|
| Task 1 (global config) | `global/config/*.java` (5개) | `auth/config/SecurityConfig.java`, `common/s3/S3Config.java`, `common/redis/RedisConfig.java`, `common/kafka/KafkaConfig.java`, `common/entity/JpaAuditingConfig.java` 삭제 |
| Task 2 (record 제거) | — | `KakaoUserInfo`, `GoogleUserInfo`, `PostCreatedEvent`, `ApiResponse.ErrorBody`, `S3Service.PresignedUrlResponse`, `FollowId`, `LikeId`, `PostTagId` 수정 + 호출부 3개 수정 |
| Task 3 (domain→entity) | `*/entity/` 파일 13개 | `*/domain/` 파일 13개 삭제 + import 수정 20개 파일 |
| Task 4 (TodoItemResponse) | `todo/dto/TodoItemResponse.java` | `TodoResponse.java` 수정 |
| Task 5 (.claude/docs) | 5개 md | 에이전트 6개 md 수정 |
| Task 6 (.claude/skills) | 5개 md | 에이전트 6개 md 수정 |
| Task 7 (규칙 추가) | — | `CLAUDE.md`, `backend.md`, `frontend.md` 수정 |

---

### Task 1: `joat.global.config` 패키지 신설 및 Config 파일 이동

**Files:**
- Create: `src/main/java/joat/global/config/SecurityConfig.java`
- Create: `src/main/java/joat/global/config/S3Config.java`
- Create: `src/main/java/joat/global/config/RedisConfig.java`
- Create: `src/main/java/joat/global/config/KafkaConfig.java`
- Create: `src/main/java/joat/global/config/JpaAuditingConfig.java`
- Delete: `src/main/java/joat/auth/config/SecurityConfig.java`
- Delete: `src/main/java/joat/common/s3/S3Config.java`
- Delete: `src/main/java/joat/common/redis/RedisConfig.java`
- Delete: `src/main/java/joat/common/kafka/KafkaConfig.java`
- Delete: `src/main/java/joat/common/entity/JpaAuditingConfig.java`

- [ ] **Step 1: `SecurityConfig.java` 생성**

```java
package joat.global.config;

import joat.auth.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/explore").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

- [ ] **Step 2: `S3Config.java` 생성**

```java
package joat.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
```

- [ ] **Step 3: `RedisConfig.java` 생성**

```java
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

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
        return template;
    }
}
```

- [ ] **Step 4: `KafkaConfig.java` 생성**

```java
package joat.global.config;

import joat.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.POST_CREATED)
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

- [ ] **Step 5: `JpaAuditingConfig.java` 생성**

```java
package joat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
```

- [ ] **Step 6: 기존 Config 파일 5개 삭제**

```bash
rm src/main/java/joat/auth/config/SecurityConfig.java
rm -r src/main/java/joat/auth/config
rm src/main/java/joat/common/s3/S3Config.java
rm src/main/java/joat/common/redis/RedisConfig.java
rm -r src/main/java/joat/common/redis
rm src/main/java/joat/common/kafka/KafkaConfig.java
rm src/main/java/joat/common/entity/JpaAuditingConfig.java
```

- [ ] **Step 7: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 2: record → `@Getter + @AllArgsConstructor` 교체

**Files:**
- Modify: `src/main/java/joat/auth/dto/KakaoUserInfo.java`
- Modify: `src/main/java/joat/auth/dto/GoogleUserInfo.java`
- Modify: `src/main/java/joat/common/kafka/event/PostCreatedEvent.java`
- Modify: `src/main/java/joat/common/response/ApiResponse.java`
- Modify: `src/main/java/joat/common/s3/S3Service.java`
- Modify: `src/main/java/joat/user/domain/FollowId.java`
- Modify: `src/main/java/joat/feed/domain/LikeId.java`
- Modify: `src/main/java/joat/tag/domain/PostTagId.java`
- Modify: `src/main/java/joat/auth/service/AuthServiceImpl.java` (call sites)
- Modify: `src/main/java/joat/tag/kafka/TagEventConsumer.java` (call sites)
- Modify: `src/main/java/joat/common/kafka/PostEventProducer.java` (call sites)

- [ ] **Step 1: `KakaoUserInfo.java` 수정**

```java
package joat.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfo {
    private String id;
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            return kakaoAccount.getProfile().getNickname();
        }
        return "갓생러";
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoAccount {
        private Profile profile;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String nickname;
    }
}
```

- [ ] **Step 2: `GoogleUserInfo.java` 수정**

```java
package joat.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    private String sub;
    private String name;
}
```

- [ ] **Step 3: `PostCreatedEvent.java` 수정**

```java
package joat.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private UUID postId;
    private UUID userId;
    private List<String> tagNames;
}
```

- [ ] **Step 4: `ApiResponse.java` inner record → inner class 수정**

```java
package joat.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorBody error;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorBody {
        private final String code;
        private final String message;
    }
}
```

- [ ] **Step 5: `S3Service.java` inner record → inner class 수정**

`S3Service.java` 파일 하단의 `record` 선언을:
```java
public record PresignedUrlResponse(String presignedUrl, String fileUrl) {}
```
아래로 교체:
```java
@Getter
@AllArgsConstructor
public static class PresignedUrlResponse {
    private final String presignedUrl;
    private final String fileUrl;
}
```
파일 상단에 Lombok import 추가:
```java
import lombok.AllArgsConstructor;
import lombok.Getter;
```

- [ ] **Step 6: `FollowId.java` record → class 수정**

```java
package joat.user.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FollowId implements Serializable {
    private UUID followerId;
    private UUID followingId;
}
```

- [ ] **Step 7: `LikeId.java` record → class 수정**

```java
package joat.feed.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LikeId implements Serializable {
    private UUID userId;
    private UUID postId;
}
```

- [ ] **Step 8: `PostTagId.java` record → class 수정**

```java
package joat.tag.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostTagId implements Serializable {
    private UUID postId;
    private UUID tagId;
}
```

- [ ] **Step 9: `AuthServiceImpl.java` 호출부 수정**

`kakaoLogin` 메서드 내:
```java
// 변경 전
.findByOauthProviderAndOauthId(OAuthProvider.kakao, info.id())
.orElseGet(() -> userRepository.save(
    User.of(info.nickname(), OAuthProvider.kakao, info.id())));

// 변경 후
.findByOauthProviderAndOauthId(OAuthProvider.kakao, info.getId())
.orElseGet(() -> userRepository.save(
    User.of(info.getNickname(), OAuthProvider.kakao, info.getId())));
```

`googleLogin` 메서드 내:
```java
// 변경 전
.findByOauthProviderAndOauthId(OAuthProvider.google, info.sub())
.orElseGet(() -> userRepository.save(
    User.of(info.name(), OAuthProvider.google, info.sub())));

// 변경 후
.findByOauthProviderAndOauthId(OAuthProvider.google, info.getSub())
.orElseGet(() -> userRepository.save(
    User.of(info.getName(), OAuthProvider.google, info.getSub())));
```

- [ ] **Step 10: `TagEventConsumer.java` 호출부 수정**

```java
// 변경 전
List<String> tagNames = event.tagNames();
...
tagService.processTags(event.postId(), tagNames);

// 변경 후
List<String> tagNames = event.getTagNames();
...
tagService.processTags(event.getPostId(), tagNames);
```

- [ ] **Step 11: `PostEventProducer.java` 호출부 수정**

```java
// 변경 전
kafkaTemplate.send(KafkaTopics.POST_CREATED, event.postId().toString(), event)

// 변경 후
kafkaTemplate.send(KafkaTopics.POST_CREATED, event.getPostId().toString(), event)
```

- [ ] **Step 12: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 3: `domain` → `entity` 패키지 rename (feed)

**Files:**
- Create: `src/main/java/joat/feed/entity/Post.java`, `PostType.java`, `Like.java`, `LikeId.java`, `Comment.java`
- Delete: `src/main/java/joat/feed/domain/` 전체
- Modify imports: `PostRepository`, `LikeRepository`, `CommentRepository`, `PostServiceImpl`, `CommentServiceImpl`, `PostResponse`, `CommentResponse`

- [ ] **Step 1: `feed/entity/` 파일 5개 생성 (package만 변경)**

각 파일 상단 `package joat.feed.domain;` → `package joat.feed.entity;`로 변경하여 복사:
- `Post.java`
- `PostType.java`
- `Like.java`
- `LikeId.java` (Task 2에서 이미 record→class 변환 완료)
- `Comment.java`

- [ ] **Step 2: `feed/domain/` 전체 삭제**

```bash
rm -r src/main/java/joat/feed/domain
```

- [ ] **Step 3: `PostRepository.java` import 수정**

`import joat.feed.domain.Post;` → `import joat.feed.entity.Post;`

- [ ] **Step 4: `LikeRepository.java` import 수정**

`import joat.feed.domain.Like;` → `import joat.feed.entity.Like;`
`import joat.feed.domain.LikeId;` → `import joat.feed.entity.LikeId;`

- [ ] **Step 5: `CommentRepository.java` import 수정**

`import joat.feed.domain.Comment;` → `import joat.feed.entity.Comment;`

- [ ] **Step 6: `PostServiceImpl.java` import 수정**

```java
import joat.feed.entity.Like;
import joat.feed.entity.LikeId;
import joat.feed.entity.Post;
```

- [ ] **Step 7: `CommentServiceImpl.java` import 수정**

```java
import joat.feed.entity.Comment;
import joat.feed.entity.Post;
```

- [ ] **Step 8: `PostResponse.java` import 수정**

```java
import joat.feed.entity.Post;
import joat.feed.entity.PostType;
```

- [ ] **Step 9: `CommentResponse.java` import 수정**

`import joat.feed.domain.Comment;` → `import joat.feed.entity.Comment;`

- [ ] **Step 10: `PostServiceTest.java` import 수정**

`import joat.feed.domain.Post;` → `import joat.feed.entity.Post;`

---

### Task 4: `domain` → `entity` 패키지 rename (user)

**Files:**
- Create: `src/main/java/joat/user/entity/User.java`, `OAuthProvider.java`, `Follow.java`, `FollowId.java`
- Delete: `src/main/java/joat/user/domain/` 전체
- Modify imports: `UserRepository`, `FollowRepository`, `UserServiceImpl`, `AuthServiceImpl`, `PostServiceImpl`, `UserProfileResponse`, 테스트 3개

- [ ] **Step 1: `user/entity/` 파일 4개 생성**

각 파일 `package joat.user.domain;` → `package joat.user.entity;`로 변경:
- `User.java`
- `OAuthProvider.java`
- `Follow.java`
- `FollowId.java` (Task 2에서 record→class 완료)

- [ ] **Step 2: `user/domain/` 전체 삭제**

```bash
rm -r src/main/java/joat/user/domain
```

- [ ] **Step 3: `UserRepository.java` import 수정**

```java
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
```

- [ ] **Step 4: `FollowRepository.java` import 수정**

```java
import joat.user.entity.Follow;
import joat.user.entity.FollowId;
```

- [ ] **Step 5: `UserServiceImpl.java` import 수정**

```java
import joat.user.entity.Follow;
import joat.user.entity.FollowId;
import joat.user.entity.User;
```

- [ ] **Step 6: `AuthServiceImpl.java` import 수정**

```java
import joat.user.entity.OAuthProvider;
import joat.user.entity.User;
```

- [ ] **Step 7: `PostServiceImpl.java` import 수정**

`import joat.user.domain.Follow;` → `import joat.user.entity.Follow;`

- [ ] **Step 8: `UserProfileResponse.java` import 수정**

`import joat.user.domain.User;` → `import joat.user.entity.User;`

- [ ] **Step 9: 테스트 파일 3개 import 수정**

`AuthServiceTest.java`: `joat.user.domain.*` → `joat.user.entity.*`
`UserServiceTest.java`: `joat.user.domain.*` → `joat.user.entity.*`
`UserTest.java`: `joat.user.domain.*` → `joat.user.entity.*`

---

### Task 5: `domain` → `entity` 패키지 rename (todo)

**Files:**
- Create: `src/main/java/joat/todo/entity/Todo.java`, `TodoItem.java`
- Delete: `src/main/java/joat/todo/domain/` 전체
- Modify imports: `TodoRepository`, `TodoItemRepository`, `TodoServiceImpl`, `TodoResponse`, `TodoServiceTest`

- [ ] **Step 1: `todo/entity/` 파일 2개 생성**

`package joat.todo.domain;` → `package joat.todo.entity;`로 변경:
- `Todo.java` (단, 내부 import `joat.common.entity.*` 는 그대로 유지)
- `TodoItem.java`

- [ ] **Step 2: `todo/domain/` 전체 삭제**

```bash
rm -r src/main/java/joat/todo/domain
```

- [ ] **Step 3: `TodoRepository.java` import 수정**

`import joat.todo.domain.Todo;` → `import joat.todo.entity.Todo;`

- [ ] **Step 4: `TodoItemRepository.java` import 수정**

`import joat.todo.domain.TodoItem;` → `import joat.todo.entity.TodoItem;`

- [ ] **Step 5: `TodoServiceImpl.java` import 수정**

```java
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
```

- [ ] **Step 6: `TodoResponse.java` import 수정**

```java
import joat.todo.entity.Todo;
import joat.todo.entity.TodoItem;
```

- [ ] **Step 7: `TodoServiceTest.java` import 수정**

`import joat.todo.domain.Todo;` → `import joat.todo.entity.Todo;`

---

### Task 6: `domain` → `entity` 패키지 rename (tag)

**Files:**
- Create: `src/main/java/joat/tag/entity/Tag.java`, `PostTag.java`, `PostTagId.java`
- Delete: `src/main/java/joat/tag/domain/` 전체
- Modify imports: `TagRepository`, `PostTagRepository`, `TagServiceImpl`, `TagResponse`, `TagServiceTest`

- [ ] **Step 1: `tag/entity/` 파일 3개 생성**

`package joat.tag.domain;` → `package joat.tag.entity;`로 변경:
- `Tag.java`
- `PostTag.java`
- `PostTagId.java` (Task 2에서 record→class 완료)

- [ ] **Step 2: `tag/domain/` 전체 삭제**

```bash
rm -r src/main/java/joat/tag/domain
```

- [ ] **Step 3: `TagRepository.java` import 수정**

`import joat.tag.domain.Tag;` → `import joat.tag.entity.Tag;`

- [ ] **Step 4: `PostTagRepository.java` import 수정**

```java
import joat.tag.entity.PostTag;
import joat.tag.entity.PostTagId;
```

- [ ] **Step 5: `TagServiceImpl.java` import 수정**

```java
import joat.tag.entity.PostTag;
import joat.tag.entity.Tag;
```

- [ ] **Step 6: `TagResponse.java` import 수정**

`import joat.tag.domain.Tag;` → `import joat.tag.entity.Tag;`

- [ ] **Step 7: `TagServiceTest.java` import 수정**

`import joat.tag.domain.Tag;` → `import joat.tag.entity.Tag;`

- [ ] **Step 8: 전체 컴파일 + 테스트 확인**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL, 모든 테스트 통과

- [ ] **Step 9: 커밋**

```bash
git add -A
git commit -m "refactor: global config 패키지 통합, record 제거, domain→entity 패키지 rename"
```

---

### Task 7: `TodoItemResponse` 분리

**Files:**
- Create: `src/main/java/joat/todo/dto/TodoItemResponse.java`
- Modify: `src/main/java/joat/todo/dto/TodoResponse.java`

- [ ] **Step 1: `TodoItemResponse.java` 생성**

```java
package joat.todo.dto;

import joat.todo.entity.TodoItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemResponse {
    private UUID id;
    private String content;
    private boolean isDone;
    private int orderIdx;

    public static TodoItemResponse from(TodoItem item) {
        return new TodoItemResponse(item.getId(), item.getContent(), item.isDone(), item.getOrderIdx());
    }
}
```

- [ ] **Step 2: `TodoResponse.java` inner class 제거 및 import 추가**

```java
package joat.todo.dto;

import joat.todo.entity.Todo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {
    private UUID id;
    private String title;
    private boolean isPublic;
    private LocalDate date;
    private List<TodoItemResponse> items;

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
            todo.getId(),
            todo.getTitle(),
            todo.isPublic(),
            todo.getDate(),
            todo.getItems().stream().map(TodoItemResponse::from).toList()
        );
    }
}
```

- [ ] **Step 3: 빌드 + 테스트 확인**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/joat/todo/dto/
git commit -m "refactor: TodoItemResponse를 TodoResponse에서 분리"
```

---

### Task 8: `.claude/docs/` 에이전트별 참조문서 생성

**Files:**
- Create: `.claude/docs/backend-docs.md`
- Create: `.claude/docs/db-docs.md`
- Create: `.claude/docs/frontend-docs.md`
- Create: `.claude/docs/infra-docs.md`
- Create: `.claude/docs/review-docs.md`
- Modify: `.claude/agents/backend.md`, `db.md`, `frontend.md`, `infra.md`, `reviewer.md`, `ceo.md`

- [ ] **Step 1: `.claude/docs/` 5개 md 파일 생성**

각 파일에 해당 에이전트가 실제로 필요한 정보(API 스펙, 스키마, 화면 목록, 환경 변수, 컨벤션 기준)를 작성.

- [ ] **Step 2: 각 `agents/*.md`에 `## 참조 문서` 섹션 추가**

```markdown
## 참조 문서

- 전체 설계 스펙: `docs/superpowers/specs/2026-05-31-gatsaeng-community-design.md`
- 에이전트 전용 참조: `.claude/docs/backend-docs.md`
- 기술 스택: `.claude/tech-stack.md`
```

---

### Task 9: `.claude/skills/` 에이전트 하네스 구축

**Files:**
- Create: `.claude/skills/backend-skill.md`
- Create: `.claude/skills/db-skill.md`
- Create: `.claude/skills/frontend-skill.md`
- Create: `.claude/skills/infra-skill.md`
- Create: `.claude/skills/review-skill.md`
- Modify: 각 `agents/*.md`에 `## 사용 가능 Skills` 섹션 추가

- [ ] **Step 1: `.claude/skills/` 5개 skill 파일 생성**

각 파일은 해당 에이전트가 작업 전에 확인해야 할 체크리스트 형식.

- [ ] **Step 2: 각 `agents/*.md`에 `## 사용 가능 Skills` 섹션 추가**

```markdown
## 사용 가능 Skills

작업 시작 전 `.claude/skills/backend-skill.md`를 참조하여 체크리스트를 확인한다.
```

---

### Task 10: 백엔드+프론트 동시 개발 규칙 추가

**Files:**
- Modify: `CLAUDE.md`
- Modify: `.claude/agents/backend.md`
- Modify: `.claude/agents/frontend.md`

- [ ] **Step 1: `CLAUDE.md`에 규칙 7 추가**

```markdown
### 7. 백엔드+프론트 동시 개발 원칙
- **백엔드 API를 작성할 때 React Native 화면과 API 연동 코드도 함께 구현한다.**
- 화면에 버튼/UI가 있는 API라면 반드시 프론트 화면도 만든다.
- 백엔드 서비스 로직 → API 엔드포인트 → RN 화면 → API 연동 순서로 완성.
```

- [ ] **Step 2: `agents/backend.md`에 규칙 추가**

`## MVP 외 기능 구현 금지` 섹션 위에:
```markdown
## 프론트엔드 병행 개발

백엔드 API를 완성하면 반드시 React Native 화면과 API 연동 코드도 함께 작성한다.
UI가 있는 기능이라면 화면 없이 백엔드만 완료 처리하지 않는다.
```

- [ ] **Step 3: `agents/frontend.md`에 규칙 추가**

```markdown
## 백엔드 연동 필수

백엔드 에이전트가 API를 완성하면 해당 API를 사용하는 화면과 연동 코드를 반드시 구현한다.
API가 새로 생기면 화면도 같이 업데이트한다.
```

- [ ] **Step 4: 최종 커밋**

```bash
git add .claude/ CLAUDE.md
git commit -m "feat: .claude docs/skills 하네스 구축, 백엔드+프론트 동시 개발 규칙 추가"
```
