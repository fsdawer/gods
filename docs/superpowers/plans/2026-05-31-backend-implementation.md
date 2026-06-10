# 갓생 커뮤니티 앱 백엔드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 3.4.5 모듈형 모놀리스로 갓생 커뮤니티 앱의 REST API를 구현한다 — auth/user/feed/todo/tag 6개 도메인.

**Architecture:** 모든 도메인이 하나의 Spring Boot 앱에 패키지로 분리. common 모듈이 공통 인프라를 제공하고, 각 도메인은 독립 패키지로 다른 도메인 Service를 인터페이스로만 참조.

**Tech Stack:** Spring Boot 3.4.5, Java 17, PostgreSQL 16, Redis 7, JPA + Hibernate, Flyway, JWT(jjwt 0.12.6), OAuth2(Kakao/Google REST), AWS S3 SDK v2, Lombok, JUnit 5 + Mockito

---

## 파일 구조 전체 맵

```
src/main/java/joat/
├── common/
│   ├── entity/BaseEntity.java
│   ├── response/ApiResponse.java
│   ├── exception/ErrorCode.java
│   ├── exception/BusinessException.java
│   ├── exception/GlobalExceptionHandler.java
│   └── s3/S3Service.java
├── auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── client/KakaoOAuthClient.java
│   ├── client/GoogleOAuthClient.java
│   ├── jwt/JwtUtil.java
│   ├── jwt/JwtFilter.java
│   ├── config/SecurityConfig.java
│   └── dto/{OAuthLoginRequest, TokenResponse, KakaoUserInfo, GoogleUserInfo}.java
├── user/
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── domain/{User, Follow, OAuthProvider}.java
│   ├── repository/{UserRepository, FollowRepository}.java
│   └── dto/{UserProfileResponse, UpdateProfileRequest, FollowListResponse}.java
├── todo/
│   ├── controller/TodoController.java
│   ├── service/TodoService.java
│   ├── domain/{Todo, TodoItem}.java
│   ├── repository/{TodoRepository, TodoItemRepository}.java
│   └── dto/{CreateTodoRequest, TodoResponse, UpdateTodoItemRequest, CertifyRequest}.java
├── feed/
│   ├── controller/PostController.java
│   ├── service/{PostService, CommentService}.java
│   ├── domain/{Post, PostType, Like, LikeId, Comment}.java
│   ├── repository/{PostRepository, LikeRepository, CommentRepository}.java
│   └── dto/{CreatePostRequest, PostResponse, CreateCommentRequest, CommentResponse, CursorResponse}.java
└── tag/
    ├── controller/TagController.java
    ├── service/TagService.java
    ├── domain/{Tag, PostTag, PostTagId}.java
    ├── repository/{TagRepository, PostTagRepository}.java
    └── dto/TagResponse.java

src/main/resources/
├── application.yaml
├── application-local.yaml
├── application-prod.yaml
└── db/migration/
    ├── V1__create_users.sql
    ├── V2__create_todos.sql
    ├── V3__create_posts.sql
    ├── V4__create_social.sql
    └── V5__create_indexes.sql

src/test/java/joat/
├── auth/{AuthServiceTest, AuthControllerTest}.java
├── user/{UserServiceTest, UserControllerTest}.java
├── todo/{TodoServiceTest, TodoControllerTest}.java
├── feed/{PostServiceTest, PostControllerTest}.java
└── tag/TagServiceTest.java
```

---

### Task 1: 프로젝트 의존성 + 로컬 환경 설정

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/application.yaml`
- Create: `src/main/resources/application-local.yaml`
- Create: `docker-compose.yml`

- [ ] **Step 1: build.gradle 의존성 추가**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'joat'
version = '0.0.1-SNAPSHOT'

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

configurations {
    compileOnly { extendsFrom annotationProcessor }
}

repositories { mavenCentral() }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.flywaydb:flyway-database-postgresql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // AWS S3
    implementation 'software.amazon.awssdk:s3:2.25.0'

    // HTTP Client (OAuth 외부 API 호출용)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'com.h2database:h2'
}

tasks.named('test') { useJUnitPlatform() }
```

- [ ] **Step 2: application.yaml 작성**

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}
  access-expiry: ${JWT_ACCESS_EXPIRY:1800}
  refresh-expiry: ${JWT_REFRESH_EXPIRY:1209600}

kakao:
  user-info-url: https://kapi.kakao.com/v2/user/me

google:
  user-info-url: https://www.googleapis.com/oauth2/v3/userinfo

aws:
  s3:
    bucket: ${AWS_S3_BUCKET}
    region: ${AWS_REGION:ap-northeast-2}
    presigned-expiry: ${S3_PRESIGNED_EXPIRY:300}
```

- [ ] **Step 3: application-local.yaml 작성**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/joat
    username: joat
    password: joat
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    show-sql: true

jwt:
  secret: local-dev-secret-key-must-be-at-least-256-bits-long-for-hs256

aws:
  s3:
    bucket: joat-local-bucket
```

- [ ] **Step 4: docker-compose.yml 작성**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: joat
      POSTGRES_USER: joat
      POSTGRES_PASSWORD: joat
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

- [ ] **Step 5: 빌드 확인**

```bash
docker-compose up -d
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add build.gradle src/main/resources/application*.yaml docker-compose.yml
git commit -m "chore: 프로젝트 의존성 및 로컬 환경 설정"
```

---

### Task 2: Common 모듈 — BaseEntity + ApiResponse + 예외처리

**Files:**
- Create: `src/main/java/joat/common/entity/BaseEntity.java`
- Create: `src/main/java/joat/common/response/ApiResponse.java`
- Create: `src/main/java/joat/common/exception/ErrorCode.java`
- Create: `src/main/java/joat/common/exception/BusinessException.java`
- Create: `src/main/java/joat/common/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/joat/common/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 테스트 먼저 작성**

`src/test/java/joat/common/exception/GlobalExceptionHandlerTest.java`

```java
package joat.common.exception;

import joat.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandlerTest.FakeController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @RestController
    static class FakeController {
        @GetMapping("/test/business-ex")
        void throwBusiness() {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Test
    void businessException은_에러코드에_맞는_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/test/business-ex"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "joat.common.exception.GlobalExceptionHandlerTest" 2>&1 | tail -5
```

Expected: 컴파일 에러 (클래스 없음)

- [ ] **Step 3: BaseEntity 구현**

`src/main/java/joat/common/entity/BaseEntity.java`

```java
package joat.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: ApiResponse 구현**

`src/main/java/joat/common/response/ApiResponse.java`

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

    public record ErrorBody(String code, String message) {}
}
```

- [ ] **Step 5: ErrorCode 구현**

`src/main/java/joat/common/exception/ErrorCode.java`

```java
package joat.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "이미 팔로우 중입니다."),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST, "팔로우 중이 아닙니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    OAUTH_FAILED(HttpStatus.BAD_REQUEST, "소셜 로그인에 실패했습니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시물을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시물 접근 권한이 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시물입니다."),
    NOT_LIKED(HttpStatus.BAD_REQUEST, "좋아요하지 않은 게시물입니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),

    // Todo
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "투두를 찾을 수 없습니다."),
    TODO_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "투두 항목을 찾을 수 없습니다."),
    TODO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "투두 접근 권한이 없습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
```

- [ ] **Step 6: BusinessException 구현**

`src/main/java/joat/common/exception/BusinessException.java`

```java
package joat.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 7: GlobalExceptionHandler 구현**

`src/main/java/joat/common/exception/GlobalExceptionHandler.java`

```java
package joat.common.exception;

import joat.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
            .body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
```

- [ ] **Step 8: JoatApplication에 JPA Auditing 활성화**

`src/main/java/joat/joat/JoatApplication.java`

```java
package joat.joat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "joat")
@EnableJpaAuditing
public class JoatApplication {
    public static void main(String[] args) {
        SpringApplication.run(JoatApplication.class, args);
    }
}
```

- [ ] **Step 9: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.common.exception.GlobalExceptionHandlerTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 10: 커밋**

```bash
git add src/
git commit -m "feat: common 모듈 — BaseEntity, ApiResponse, 예외처리"
```

---

### Task 3: Flyway 마이그레이션 — DB 스키마 생성

**Files:**
- Create: `src/main/resources/db/migration/V1__create_users.sql`
- Create: `src/main/resources/db/migration/V2__create_todos.sql`
- Create: `src/main/resources/db/migration/V3__create_posts.sql`
- Create: `src/main/resources/db/migration/V4__create_social.sql`
- Create: `src/main/resources/db/migration/V5__create_indexes.sql`

> ⚠️ AI는 이 SQL 파일을 생성만 한다. `./gradlew bootRun` 또는 Flyway migrate는 **사람이 직접 실행**한다.

- [ ] **Step 1: V1__create_users.sql**

```sql
CREATE TYPE oauth_provider AS ENUM ('kakao', 'google');

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname          VARCHAR(50) NOT NULL,
    profile_image_url TEXT,
    bio               TEXT,
    oauth_provider    oauth_provider NOT NULL,
    oauth_id          VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (oauth_provider, oauth_id)
);
```

- [ ] **Step 2: V2__create_todos.sql**

```sql
CREATE TABLE todos (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(100) NOT NULL,
    is_public  BOOLEAN NOT NULL DEFAULT false,
    date       DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE todo_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    todo_id    UUID NOT NULL REFERENCES todos(id) ON DELETE CASCADE,
    content    VARCHAR(200) NOT NULL,
    is_done    BOOLEAN NOT NULL DEFAULT false,
    order_idx  INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```

- [ ] **Step 3: V3__create_posts.sql**

```sql
CREATE TYPE post_type AS ENUM ('free', 'todo_cert');

CREATE TABLE posts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          post_type NOT NULL DEFAULT 'free',
    content       TEXT NOT NULL,
    image_urls    TEXT[] NOT NULL DEFAULT '{}',
    todo_id       UUID REFERENCES todos(id) ON DELETE SET NULL,
    like_count    INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES comments(id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    post_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);
```

- [ ] **Step 4: V4__create_social.sql**

```sql
CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE TABLE likes (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);
```

- [ ] **Step 5: V5__create_indexes.sql**

```sql
CREATE INDEX idx_posts_user_created  ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_created       ON posts(created_at DESC);
CREATE INDEX idx_comments_post       ON comments(post_id, created_at ASC);
CREATE INDEX idx_comments_parent     ON comments(parent_id);
CREATE INDEX idx_todos_user_date     ON todos(user_id, date DESC);
CREATE INDEX idx_follows_follower    ON follows(follower_id);
CREATE INDEX idx_follows_following   ON follows(following_id);
CREATE INDEX idx_post_tags_tag       ON post_tags(tag_id);
CREATE INDEX idx_tags_name           ON tags(name text_pattern_ops);
```

- [ ] **Step 6: 커밋 (실행은 사람이)**

```bash
git add src/main/resources/db/
git commit -m "feat: Flyway 마이그레이션 스크립트 V1-V5 작성"
```

---

### Task 4: User 도메인 — 엔티티 + Repository

**Files:**
- Create: `src/main/java/joat/user/domain/OAuthProvider.java`
- Create: `src/main/java/joat/user/domain/User.java`
- Create: `src/main/java/joat/user/domain/Follow.java`
- Create: `src/main/java/joat/user/repository/UserRepository.java`
- Create: `src/main/java/joat/user/repository/FollowRepository.java`

- [ ] **Step 1: OAuthProvider enum**

`src/main/java/joat/user/domain/OAuthProvider.java`

```java
package joat.user.domain;

public enum OAuthProvider { kakao, google }
```

- [ ] **Step 2: User 엔티티**

`src/main/java/joat/user/domain/User.java`

```java
package joat.user.domain;

import jakarta.persistence.*;
import joat.common.entity.BaseEntity;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nickname;

    private String profileImageUrl;

    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider oauthProvider;

    @Column(nullable = false)
    private String oauthId;

    public static User of(String nickname, OAuthProvider provider, String oauthId) {
        User user = new User();
        user.nickname = nickname;
        user.oauthProvider = provider;
        user.oauthId = oauthId;
        return user;
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (bio != null) this.bio = bio;
    }
}
```

- [ ] **Step 3: Follow 엔티티**

`src/main/java/joat/user/domain/Follow.java`

```java
package joat.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(FollowId.class)
public class Follow {

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "following_id")
    private UUID followingId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Follow of(UUID followerId, UUID followingId) {
        Follow f = new Follow();
        f.followerId = followerId;
        f.followingId = followingId;
        return f;
    }
}
```

`src/main/java/joat/user/domain/FollowId.java`

```java
package joat.user.domain;

import java.io.Serializable;
import java.util.UUID;

public record FollowId(UUID followerId, UUID followingId) implements Serializable {}
```

- [ ] **Step 4: Repository 인터페이스**

`src/main/java/joat/user/repository/UserRepository.java`

```java
package joat.user.repository;

import joat.user.domain.OAuthProvider;
import joat.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider provider, String oauthId);
}
```

`src/main/java/joat/user/repository/FollowRepository.java`

```java
package joat.user.repository;

import joat.user.domain.Follow;
import joat.user.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    List<Follow> findByFollowerId(UUID followerId);
    List<Follow> findByFollowingId(UUID followingId);
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
```

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/joat/user/
git commit -m "feat: user 도메인 엔티티 및 repository"
```

---

### Task 5: Auth — JWT + Security 설정

**Files:**
- Create: `src/main/java/joat/auth/jwt/JwtUtil.java`
- Create: `src/main/java/joat/auth/jwt/JwtFilter.java`
- Create: `src/main/java/joat/auth/config/SecurityConfig.java`
- Test: `src/test/java/joat/auth/jwt/JwtUtilTest.java`

- [ ] **Step 1: JwtUtil 테스트 작성**

`src/test/java/joat/auth/jwt/JwtUtilTest.java`

```java
package joat.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, 30L, 1209600L);
    }

    @Test
    void 액세스_토큰을_생성하고_파싱할_수_있다() {
        String token = jwtUtil.generateAccessToken(userId);
        UUID parsed = jwtUtil.parseUserId(token);
        assertThat(parsed).isEqualTo(userId);
    }

    @Test
    void 만료된_토큰은_예외를_던진다() {
        JwtUtil shortLived = new JwtUtil(secret, 0L, 0L);
        String token = shortLived.generateAccessToken(userId);
        assertThatThrownBy(() -> shortLived.parseUserId(token))
            .isInstanceOf(joat.common.exception.BusinessException.class);
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "joat.auth.jwt.JwtUtilTest" 2>&1 | tail -5
```

Expected: 컴파일 에러

- [ ] **Step 3: JwtUtil 구현**

`src/main/java/joat/auth/jwt/JwtUtil.java`

```java
package joat.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpirySeconds;
    private final long refreshExpirySeconds;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiry}") long accessExpirySeconds,
        @Value("${jwt.refresh-expiry}") long refreshExpirySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirySeconds = accessExpirySeconds;
        this.refreshExpirySeconds = refreshExpirySeconds;
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, accessExpirySeconds);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, refreshExpirySeconds);
    }

    public UUID parseUserId(String token) {
        try {
            String sub = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
            return UUID.fromString(sub);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String buildToken(UUID userId, long expirySeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirySeconds * 1000))
            .signWith(key)
            .compact();
    }
}
```

- [ ] **Step 4: JwtFilter 구현**

`src/main/java/joat/auth/jwt/JwtFilter.java`

```java
package joat.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import joat.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                UUID userId = jwtUtil.parseUserId(token);
                var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BusinessException ignored) {
                // 인증 실패 시 SecurityContext 비움 — 403은 Security가 처리
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 5: SecurityConfig 구현**

`src/main/java/joat/auth/config/SecurityConfig.java`

```java
package joat.auth.config;

import joat.auth.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
            .csrf(c -> c.disable())
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

- [ ] **Step 6: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.auth.jwt.JwtUtilTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/joat/auth/ src/test/java/joat/auth/
git commit -m "feat: JWT 생성/검증 및 Security 필터 설정"
```

---

### Task 6: Auth — OAuth 로그인 + 토큰 발급 API

**Files:**
- Create: `src/main/java/joat/auth/client/KakaoOAuthClient.java`
- Create: `src/main/java/joat/auth/client/GoogleOAuthClient.java`
- Create: `src/main/java/joat/auth/dto/*.java` (4개)
- Create: `src/main/java/joat/auth/service/AuthService.java`
- Create: `src/main/java/joat/auth/controller/AuthController.java`
- Test: `src/test/java/joat/auth/service/AuthServiceTest.java`

- [ ] **Step 1: DTO 작성**

`src/main/java/joat/auth/dto/OAuthLoginRequest.java`
```java
package joat.auth.dto;
import jakarta.validation.constraints.NotBlank;
public record OAuthLoginRequest(@NotBlank String accessToken) {}
```

`src/main/java/joat/auth/dto/TokenResponse.java`
```java
package joat.auth.dto;
public record TokenResponse(String accessToken, String refreshToken) {}
```

`src/main/java/joat/auth/dto/KakaoUserInfo.java`
```java
package joat.auth.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
public record KakaoUserInfo(
    String id,
    @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(Profile profile) {}
    public record Profile(String nickname) {}
    public String nickname() {
        return kakaoAccount() != null && kakaoAccount().profile() != null
            ? kakaoAccount().profile().nickname() : "갓생러";
    }
}
```

`src/main/java/joat/auth/dto/GoogleUserInfo.java`
```java
package joat.auth.dto;
public record GoogleUserInfo(String sub, String name) {}
```

- [ ] **Step 2: OAuth 클라이언트 작성**

`src/main/java/joat/auth/client/KakaoOAuthClient.java`

```java
package joat.auth.client;

import joat.auth.dto.KakaoUserInfo;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final WebClient webClient = WebClient.create();

    @Value("${kakao.user-info-url}")
    private String userInfoUrl;

    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            return webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfo.class)
                .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }
}
```

`src/main/java/joat/auth/client/GoogleOAuthClient.java`

```java
package joat.auth.client;

import joat.auth.dto.GoogleUserInfo;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private final WebClient webClient = WebClient.create();

    @Value("${google.user-info-url}")
    private String userInfoUrl;

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            return webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }
}
```

- [ ] **Step 3: AuthService 테스트 작성**

`src/test/java/joat/auth/service/AuthServiceTest.java`

```java
package joat.auth.service;

import joat.auth.client.KakaoOAuthClient;
import joat.auth.dto.KakaoUserInfo;
import joat.auth.dto.TokenResponse;
import joat.auth.jwt.JwtUtil;
import joat.user.domain.OAuthProvider;
import joat.user.domain.User;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock KakaoOAuthClient kakaoOAuthClient;
    @Mock UserRepository userRepository;
    @Mock JwtUtil jwtUtil;
    @Mock StringRedisTemplate redisTemplate;

    @Test
    void 카카오_로그인_신규유저는_가입_후_토큰을_반환한다() {
        var userInfo = new KakaoUserInfo("kakao-123",
            new KakaoUserInfo.KakaoAccount(new KakaoUserInfo.Profile("갓생러")));
        given(kakaoOAuthClient.getUserInfo("token")).willReturn(userInfo);
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.kakao, "kakao-123"))
            .willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtUtil.generateAccessToken(any())).willReturn("access");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refresh");
        given(redisTemplate.opsForValue()).willReturn(mock());

        TokenResponse result = authService.kakaoLogin("token");

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        then(userRepository).should().save(any(User.class));
    }
}
```

- [ ] **Step 4: AuthService 구현**

`src/main/java/joat/auth/service/AuthService.java`

```java
package joat.auth.service;

import joat.auth.client.GoogleOAuthClient;
import joat.auth.client.KakaoOAuthClient;
import joat.auth.dto.TokenResponse;
import joat.auth.jwt.JwtUtil;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.user.domain.OAuthProvider;
import joat.user.domain.User;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoClient;
    private final GoogleOAuthClient googleClient;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    @Transactional
    public TokenResponse kakaoLogin(String accessToken) {
        var info = kakaoClient.getUserInfo(accessToken);
        User user = userRepository
            .findByOauthProviderAndOauthId(OAuthProvider.kakao, info.id())
            .orElseGet(() -> userRepository.save(
                User.of(info.nickname(), OAuthProvider.kakao, info.id())));
        return issueTokens(user.getId());
    }

    @Transactional
    public TokenResponse googleLogin(String accessToken) {
        var info = googleClient.getUserInfo(accessToken);
        User user = userRepository
            .findByOauthProviderAndOauthId(OAuthProvider.google, info.sub())
            .orElseGet(() -> userRepository.save(
                User.of(info.name(), OAuthProvider.google, info.sub())));
        return issueTokens(user.getId());
    }

    public TokenResponse refresh(String refreshToken) {
        UUID userId = jwtUtil.parseUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (!refreshToken.equals(stored)) throw new BusinessException(ErrorCode.INVALID_TOKEN);
        return issueTokens(userId);
    }

    public void logout(UUID userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    private TokenResponse issueTokens(UUID userId) {
        String access = jwtUtil.generateAccessToken(userId);
        String refresh = jwtUtil.generateRefreshToken(userId);
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + userId, refresh, Duration.ofSeconds(1209600));
        return new TokenResponse(access, refresh);
    }
}
```

- [ ] **Step 5: AuthController 구현**

`src/main/java/joat/auth/controller/AuthController.java`

```java
package joat.auth.controller;

import joat.auth.dto.OAuthLoginRequest;
import joat.auth.dto.TokenResponse;
import joat.auth.service.AuthService;
import joat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/oauth/kakao")
    public ApiResponse<TokenResponse> kakaoLogin(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.kakaoLogin(req.accessToken()));
    }

    @PostMapping("/oauth/google")
    public ApiResponse<TokenResponse> googleLogin(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.googleLogin(req.accessToken()));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody OAuthLoginRequest req) {
        return ApiResponse.ok(authService.refresh(req.accessToken()));
    }

    @DeleteMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UUID userId) {
        authService.logout(userId);
        return ApiResponse.ok();
    }
}
```

- [ ] **Step 6: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.auth.service.AuthServiceTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/joat/auth/ src/test/java/joat/auth/
git commit -m "feat: OAuth 카카오/구글 로그인 및 JWT 발급 API"
```

---

### Task 7: User — 프로필 + 팔로우 API

**Files:**
- Create: `src/main/java/joat/user/dto/*.java` (3개)
- Create: `src/main/java/joat/user/service/UserService.java`
- Create: `src/main/java/joat/user/controller/UserController.java`
- Test: `src/test/java/joat/user/service/UserServiceTest.java`

- [ ] **Step 1: DTO 작성**

`src/main/java/joat/user/dto/UserProfileResponse.java`
```java
package joat.user.dto;
import joat.user.domain.User;
import java.util.UUID;
public record UserProfileResponse(
    UUID id, String nickname, String profileImageUrl, String bio
) {
    public static UserProfileResponse from(User u) {
        return new UserProfileResponse(u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getBio());
    }
}
```

`src/main/java/joat/user/dto/UpdateProfileRequest.java`
```java
package joat.user.dto;
public record UpdateProfileRequest(String nickname, String profileImageUrl, String bio) {}
```

`src/main/java/joat/user/dto/FollowListResponse.java`
```java
package joat.user.dto;
import java.util.List;
import java.util.UUID;
public record FollowListResponse(List<UUID> userIds) {}
```

- [ ] **Step 2: UserService 테스트 작성**

`src/test/java/joat/user/service/UserServiceTest.java`

```java
package joat.user.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.user.domain.Follow;
import joat.user.domain.User;
import joat.user.domain.OAuthProvider;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks UserService userService;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;

    @Test
    void 자기자신을_팔로우하면_예외가_발생한다() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> userService.follow(id, id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("자기 자신");
    }

    @Test
    void 이미_팔로우_중이면_예외가_발생한다() {
        UUID me = UUID.randomUUID(), target = UUID.randomUUID();
        given(userRepository.findById(target))
            .willReturn(Optional.of(User.of("target", OAuthProvider.kakao, "id")));
        given(followRepository.existsByFollowerIdAndFollowingId(me, target)).willReturn(true);

        assertThatThrownBy(() -> userService.follow(me, target))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.ALREADY_FOLLOWING);
    }
}
```

- [ ] **Step 3: UserService 구현**

`src/main/java/joat/user/service/UserService.java`

```java
package joat.user.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.user.domain.Follow;
import joat.user.domain.FollowId;
import joat.user.domain.User;
import joat.user.dto.*;
import joat.user.repository.FollowRepository;
import joat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public UserProfileResponse getProfile(UUID userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = findUser(userId);
        user.updateProfile(req.nickname(), req.profileImageUrl(), req.bio());
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        findUser(followingId);
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId))
            throw new BusinessException(ErrorCode.ALREADY_FOLLOWING);
        followRepository.save(Follow.of(followerId, followingId));
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId))
            throw new BusinessException(ErrorCode.NOT_FOLLOWING);
        followRepository.deleteById(new FollowId(followerId, followingId));
    }

    public FollowListResponse getFollowers(UUID userId) {
        List<UUID> ids = followRepository.findByFollowingId(userId)
            .stream().map(Follow::getFollowerId).toList();
        return new FollowListResponse(ids);
    }

    public FollowListResponse getFollowing(UUID userId) {
        List<UUID> ids = followRepository.findByFollowerId(userId)
            .stream().map(Follow::getFollowingId).toList();
        return new FollowListResponse(ids);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
```

- [ ] **Step 4: UserController 구현**

`src/main/java/joat/user/controller/UserController.java`

```java
package joat.user.controller;

import joat.common.response.ApiResponse;
import joat.user.dto.*;
import joat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> update(
        @AuthenticationPrincipal UUID userId,
        @RequestBody UpdateProfileRequest req
    ) {
        return ApiResponse.ok(userService.updateProfile(userId, req));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PostMapping("/{userId}/follow")
    public ApiResponse<Void> follow(@AuthenticationPrincipal UUID me, @PathVariable UUID userId) {
        userService.follow(me, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{userId}/follow")
    public ApiResponse<Void> unfollow(@AuthenticationPrincipal UUID me, @PathVariable UUID userId) {
        userService.unfollow(me, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<FollowListResponse> followers(@PathVariable UUID userId) {
        return ApiResponse.ok(userService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ApiResponse<FollowListResponse> following(@PathVariable UUID userId) {
        return ApiResponse.ok(userService.getFollowing(userId));
    }
}
```

- [ ] **Step 5: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.user.service.UserServiceTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/joat/user/ src/test/java/joat/user/
git commit -m "feat: user 프로필 조회/수정 및 팔로우 API"
```

---

### Task 8: Todo 도메인

**Files:**
- Create: `src/main/java/joat/todo/domain/{Todo, TodoItem}.java`
- Create: `src/main/java/joat/todo/repository/{TodoRepository, TodoItemRepository}.java`
- Create: `src/main/java/joat/todo/dto/*.java` (4개)
- Create: `src/main/java/joat/todo/service/TodoService.java`
- Create: `src/main/java/joat/todo/controller/TodoController.java`
- Test: `src/test/java/joat/todo/service/TodoServiceTest.java`

- [ ] **Step 1: Todo, TodoItem 엔티티**

`src/main/java/joat/todo/domain/Todo.java`

```java
package joat.todo.domain;

import jakarta.persistence.*;
import joat.common.entity.BaseEntity;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "todos")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean isPublic;

    @Column(nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIdx ASC")
    private List<TodoItem> items = new ArrayList<>();

    public static Todo of(UUID userId, String title, boolean isPublic, LocalDate date) {
        Todo t = new Todo();
        t.userId = userId;
        t.title = title;
        t.isPublic = isPublic;
        t.date = date;
        return t;
    }

    public void update(String title, Boolean isPublic) {
        if (title != null) this.title = title;
        if (isPublic != null) this.isPublic = isPublic;
    }

    public void validateOwner(UUID requesterId) {
        if (!userId.equals(requesterId))
            throw new joat.common.exception.BusinessException(joat.common.exception.ErrorCode.TODO_ACCESS_DENIED);
    }
}
```

`src/main/java/joat/todo/domain/TodoItem.java`

```java
package joat.todo.domain;

import jakarta.persistence.*;
import joat.common.entity.BaseEntity;
import lombok.*;

import java.util.UUID;

@Entity @Table(name = "todo_items")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private boolean isDone;

    @Column(nullable = false)
    private int orderIdx;

    public static TodoItem of(Todo todo, String content, int orderIdx) {
        TodoItem item = new TodoItem();
        item.todo = todo;
        item.content = content;
        item.orderIdx = orderIdx;
        return item;
    }

    public void toggleDone(boolean done) { this.isDone = done; }
}
```

- [ ] **Step 2: Repository + DTO**

`src/main/java/joat/todo/repository/TodoRepository.java`
```java
package joat.todo.repository;
import joat.todo.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface TodoRepository extends JpaRepository<Todo, UUID> {
    List<Todo> findByUserIdAndDate(UUID userId, LocalDate date);
    List<Todo> findByUserIdAndIsPublicTrue(UUID userId);
}
```

`src/main/java/joat/todo/repository/TodoItemRepository.java`
```java
package joat.todo.repository;
import joat.todo.domain.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TodoItemRepository extends JpaRepository<TodoItem, UUID> {}
```

`src/main/java/joat/todo/dto/CreateTodoRequest.java`
```java
package joat.todo.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
public record CreateTodoRequest(
    @NotBlank String title,
    boolean isPublic,
    @NotNull LocalDate date,
    List<String> items
) {}
```

`src/main/java/joat/todo/dto/TodoResponse.java`
```java
package joat.todo.dto;
import joat.todo.domain.Todo;
import joat.todo.domain.TodoItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record TodoResponse(
    UUID id, String title, boolean isPublic, LocalDate date,
    List<ItemResponse> items
) {
    public record ItemResponse(UUID id, String content, boolean isDone, int orderIdx) {
        public static ItemResponse from(TodoItem i) {
            return new ItemResponse(i.getId(), i.getContent(), i.isDone(), i.getOrderIdx());
        }
    }
    public static TodoResponse from(Todo t) {
        return new TodoResponse(t.getId(), t.getTitle(), t.isPublic(), t.getDate(),
            t.getItems().stream().map(ItemResponse::from).toList());
    }
}
```

`src/main/java/joat/todo/dto/UpdateTodoItemRequest.java`
```java
package joat.todo.dto;
public record UpdateTodoItemRequest(boolean isDone) {}
```

`src/main/java/joat/todo/dto/CertifyRequest.java`
```java
package joat.todo.dto;
import java.util.List;
public record CertifyRequest(String content, List<String> imageUrls) {}
```

- [ ] **Step 3: TodoService 테스트 작성**

`src/test/java/joat/todo/service/TodoServiceTest.java`

```java
package joat.todo.service;

import joat.common.exception.BusinessException;
import joat.todo.domain.Todo;
import joat.todo.repository.TodoItemRepository;
import joat.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @InjectMocks TodoService todoService;
    @Mock TodoRepository todoRepository;
    @Mock TodoItemRepository todoItemRepository;

    @Test
    void 다른_유저의_투두를_수정하면_예외가_발생한다() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Todo todo = Todo.of(owner, "투두", false, LocalDate.now());
        given(todoRepository.findById(todo.getId())).willReturn(Optional.of(todo));

        assertThatThrownBy(() -> todoService.deleteTodo(todo.getId(), other))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 4: TodoService 구현**

`src/main/java/joat/todo/service/TodoService.java`

```java
package joat.todo.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.todo.domain.Todo;
import joat.todo.domain.TodoItem;
import joat.todo.dto.*;
import joat.todo.repository.TodoItemRepository;
import joat.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final TodoItemRepository todoItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TodoResponse createTodo(UUID userId, CreateTodoRequest req) {
        Todo todo = todoRepository.save(
            Todo.of(userId, req.title(), req.isPublic(), req.date()));
        if (req.items() != null) {
            for (int i = 0; i < req.items().size(); i++) {
                todoItemRepository.save(TodoItem.of(todo, req.items().get(i), i));
            }
        }
        return TodoResponse.from(todo);
    }

    public List<TodoResponse> getMyTodos(UUID userId, LocalDate date) {
        return todoRepository.findByUserIdAndDate(userId, date)
            .stream().map(TodoResponse::from).toList();
    }

    public List<TodoResponse> getPublicTodos(UUID userId) {
        return todoRepository.findByUserIdAndIsPublicTrue(userId)
            .stream().map(TodoResponse::from).toList();
    }

    @Transactional
    public void deleteTodo(UUID todoId, UUID requesterId) {
        Todo todo = findTodo(todoId);
        todo.validateOwner(requesterId);
        todoRepository.delete(todo);
    }

    @Transactional
    public TodoResponse checkItem(UUID todoId, UUID itemId, UUID requesterId, boolean isDone) {
        Todo todo = findTodo(todoId);
        todo.validateOwner(requesterId);
        TodoItem item = todoItemRepository.findById(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TODO_ITEM_NOT_FOUND));
        item.toggleDone(isDone);
        return TodoResponse.from(todo);
    }

    public Todo findTodo(UUID todoId) {
        return todoRepository.findById(todoId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
    }
}
```

- [ ] **Step 5: TodoController 구현**

`src/main/java/joat/todo/controller/TodoController.java`

```java
package joat.todo.controller;

import jakarta.validation.Valid;
import joat.common.response.ApiResponse;
import joat.todo.dto.*;
import joat.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ApiResponse<TodoResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateTodoRequest req
    ) {
        return ApiResponse.ok(todoService.createTodo(userId, req));
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> getMine(
        @AuthenticationPrincipal UUID userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(todoService.getMyTodos(userId, date));
    }

    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> delete(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId
    ) {
        todoService.deleteTodo(todoId, userId);
        return ApiResponse.ok();
    }

    @PatchMapping("/{todoId}/items/{itemId}")
    public ApiResponse<TodoResponse> checkItem(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID todoId,
        @PathVariable UUID itemId,
        @RequestBody UpdateTodoItemRequest req
    ) {
        return ApiResponse.ok(todoService.checkItem(todoId, itemId, userId, req.isDone()));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<TodoResponse>> getPublic(@PathVariable UUID userId) {
        return ApiResponse.ok(todoService.getPublicTodos(userId));
    }
}
```

- [ ] **Step 6: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.todo.service.TodoServiceTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/joat/todo/ src/test/java/joat/todo/
git commit -m "feat: todo 도메인 — 투두리스트 CRUD 및 항목 체크 API"
```

---

### Task 9: Feed 도메인 — Post + Like + Comment

**Files:**
- Create: `src/main/java/joat/feed/domain/{Post, PostType, Like, LikeId, Comment}.java`
- Create: `src/main/java/joat/feed/repository/{PostRepository, LikeRepository, CommentRepository}.java`
- Create: `src/main/java/joat/feed/dto/*.java` (5개)
- Create: `src/main/java/joat/feed/service/{PostService, CommentService}.java`
- Create: `src/main/java/joat/feed/controller/PostController.java`
- Test: `src/test/java/joat/feed/service/PostServiceTest.java`

- [ ] **Step 1: 도메인 엔티티 작성**

`src/main/java/joat/feed/domain/PostType.java`
```java
package joat.feed.domain;
public enum PostType { free, todo_cert }
```

`src/main/java/joat/feed/domain/Post.java`

```java
package joat.feed.domain;

import jakarta.persistence.*;
import joat.common.entity.BaseEntity;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity @Table(name = "posts")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private String[] imageUrls = new String[0];

    private UUID todoId;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int commentCount;

    public static Post free(UUID userId, String content, String[] imageUrls) {
        Post p = new Post();
        p.userId = userId;
        p.type = PostType.free;
        p.content = content;
        p.imageUrls = imageUrls != null ? imageUrls : new String[0];
        return p;
    }

    public static Post todoCert(UUID userId, String content, String[] imageUrls, UUID todoId) {
        Post p = free(userId, content, imageUrls);
        p.type = PostType.todo_cert;
        p.todoId = todoId;
        return p;
    }

    public void validateOwner(UUID requesterId) {
        if (!userId.equals(requesterId)) throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
    }

    public void incrementLike() { this.likeCount++; }
    public void decrementLike() { this.likeCount = Math.max(0, this.likeCount - 1); }
    public void incrementComment() { this.commentCount++; }
    public void decrementComment() { this.commentCount = Math.max(0, this.commentCount - 1); }
}
```

`src/main/java/joat/feed/domain/LikeId.java`
```java
package joat.feed.domain;
import java.io.Serializable;
import java.util.UUID;
public record LikeId(UUID userId, UUID postId) implements Serializable {}
```

`src/main/java/joat/feed/domain/Like.java`
```java
package joat.feed.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "likes")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(LikeId.class)
public class Like {
    @Id private UUID userId;
    @Id private UUID postId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Like of(UUID userId, UUID postId) {
        Like l = new Like();
        l.userId = userId;
        l.postId = postId;
        return l;
    }
}
```

`src/main/java/joat/feed/domain/Comment.java`
```java
package joat.feed.domain;
import jakarta.persistence.*;
import joat.common.entity.BaseEntity;
import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "comments")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID postId;
    @Column(nullable = false) private UUID userId;
    private UUID parentId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;

    public static Comment of(UUID postId, UUID userId, UUID parentId, String content) {
        Comment c = new Comment();
        c.postId = postId;
        c.userId = userId;
        c.parentId = parentId;
        c.content = content;
        return c;
    }

    public void validateOwner(UUID requesterId) {
        if (!userId.equals(requesterId)) throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
    }
}
```

- [ ] **Step 2: Repository 작성**

`src/main/java/joat/feed/repository/PostRepository.java`
```java
package joat.feed.repository;
import joat.feed.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds ORDER BY p.createdAt DESC")
    Slice<Post> findFeed(java.util.List<UUID> userIds, Pageable pageable);

    Slice<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

`src/main/java/joat/feed/repository/LikeRepository.java`
```java
package joat.feed.repository;
import joat.feed.domain.Like;
import joat.feed.domain.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface LikeRepository extends JpaRepository<Like, LikeId> {
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
}
```

`src/main/java/joat/feed/repository/CommentRepository.java`
```java
package joat.feed.repository;
import joat.feed.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
```

- [ ] **Step 3: DTO 작성**

`src/main/java/joat/feed/dto/CreatePostRequest.java`
```java
package joat.feed.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
public record CreatePostRequest(
    @NotBlank String content,
    List<String> imageUrls,
    List<String> tagNames,
    UUID todoId
) {}
```

`src/main/java/joat/feed/dto/PostResponse.java`
```java
package joat.feed.dto;
import joat.feed.domain.Post;
import joat.feed.domain.PostType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
public record PostResponse(
    UUID id, UUID userId, PostType type, String content,
    List<String> imageUrls, UUID todoId,
    int likeCount, int commentCount, LocalDateTime createdAt
) {
    public static PostResponse from(Post p) {
        return new PostResponse(
            p.getId(), p.getUserId(), p.getType(), p.getContent(),
            Arrays.asList(p.getImageUrls()), p.getTodoId(),
            p.getLikeCount(), p.getCommentCount(), p.getCreatedAt());
    }
}
```

`src/main/java/joat/feed/dto/CursorResponse.java`
```java
package joat.feed.dto;
import java.util.List;
import java.util.UUID;
public record CursorResponse<T>(List<T> data, UUID nextCursor, boolean hasNext) {}
```

`src/main/java/joat/feed/dto/CreateCommentRequest.java`
```java
package joat.feed.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record CreateCommentRequest(@NotBlank String content, UUID parentId) {}
```

`src/main/java/joat/feed/dto/CommentResponse.java`
```java
package joat.feed.dto;
import joat.feed.domain.Comment;
import java.time.LocalDateTime;
import java.util.UUID;
public record CommentResponse(
    UUID id, UUID userId, UUID parentId, String content, LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getUserId(), c.getParentId(), c.getContent(), c.getCreatedAt());
    }
}
```

- [ ] **Step 4: PostService 테스트**

`src/test/java/joat/feed/service/PostServiceTest.java`

```java
package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.feed.domain.Post;
import joat.feed.repository.*;
import joat.tag.service.TagService;
import joat.user.repository.FollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks PostService postService;
    @Mock PostRepository postRepository;
    @Mock LikeRepository likeRepository;
    @Mock FollowRepository followRepository;
    @Mock TagService tagService;

    @Test
    void 다른_유저_게시물을_삭제하면_예외가_발생한다() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Post post = Post.free(owner, "content", null);
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(post.getId(), other))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 이미_좋아요한_게시물을_다시_좋아요하면_예외가_발생한다() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = Post.free(userId, "content", null);
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndPostId(userId, postId)).willReturn(true);

        assertThatThrownBy(() -> postService.like(postId, userId))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 5: PostService 구현**

`src/main/java/joat/feed/service/PostService.java`

```java
package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.domain.*;
import joat.feed.dto.*;
import joat.feed.repository.*;
import joat.tag.service.TagService;
import joat.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final TagService tagService;

    @Transactional
    public PostResponse createPost(UUID userId, CreatePostRequest req) {
        String[] images = req.imageUrls() != null ? req.imageUrls().toArray(new String[0]) : new String[0];
        Post post = req.todoId() != null
            ? Post.todoCert(userId, req.content(), images, req.todoId())
            : Post.free(userId, req.content(), images);
        Post saved = postRepository.save(post);
        if (req.tagNames() != null) tagService.attachTags(saved, req.tagNames());
        return PostResponse.from(saved);
    }

    public CursorResponse<PostResponse> getFeed(UUID userId, UUID cursor, int limit) {
        List<UUID> followingIds = followRepository.findByFollowerId(userId)
            .stream().map(f -> f.getFollowingId()).toList();
        followingIds = new java.util.ArrayList<>(followingIds);
        ((java.util.ArrayList<UUID>) followingIds).add(userId);

        Slice<Post> slice = postRepository.findFeed(followingIds, PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    public CursorResponse<PostResponse> getExplore(UUID cursor, int limit) {
        Slice<Post> slice = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit + 1));
        return buildCursor(slice, limit);
    }

    public PostResponse getPost(UUID postId) {
        return PostResponse.from(findPost(postId));
    }

    @Transactional
    public void deletePost(UUID postId, UUID requesterId) {
        Post post = findPost(postId);
        post.validateOwner(requesterId);
        postRepository.delete(post);
    }

    @Transactional
    public void like(UUID postId, UUID userId) {
        Post post = findPost(postId);
        if (likeRepository.existsByUserIdAndPostId(userId, postId))
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        likeRepository.save(Like.of(userId, postId));
        post.incrementLike();
    }

    @Transactional
    public void unlike(UUID postId, UUID userId) {
        Post post = findPost(postId);
        if (!likeRepository.existsByUserIdAndPostId(userId, postId))
            throw new BusinessException(ErrorCode.NOT_LIKED);
        likeRepository.deleteById(new LikeId(userId, postId));
        post.decrementLike();
    }

    private Post findPost(UUID id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private CursorResponse<PostResponse> buildCursor(Slice<Post> slice, int limit) {
        List<Post> content = slice.getContent();
        boolean hasNext = content.size() > limit;
        List<Post> data = hasNext ? content.subList(0, limit) : content;
        UUID nextCursor = hasNext ? data.get(data.size() - 1).getId() : null;
        return new CursorResponse<>(data.stream().map(PostResponse::from).toList(), nextCursor, hasNext);
    }
}
```

- [ ] **Step 6: CommentService 구현**

`src/main/java/joat/feed/service/CommentService.java`

```java
package joat.feed.service;

import joat.common.exception.BusinessException;
import joat.common.exception.ErrorCode;
import joat.feed.domain.Comment;
import joat.feed.domain.Post;
import joat.feed.dto.CommentResponse;
import joat.feed.dto.CreateCommentRequest;
import joat.feed.repository.CommentRepository;
import joat.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentResponse> getComments(UUID postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
            .stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest req) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Comment comment = commentRepository.save(
            Comment.of(postId, userId, req.parentId(), req.content()));
        post.incrementComment();
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(UUID postId, UUID commentId, UUID requesterId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.validateOwner(requesterId);
        commentRepository.delete(comment);
        post.decrementComment();
    }
}
```

- [ ] **Step 7: PostController 구현**

`src/main/java/joat/feed/controller/PostController.java`

```java
package joat.feed.controller;

import jakarta.validation.Valid;
import joat.common.response.ApiResponse;
import joat.feed.dto.*;
import joat.feed.service.CommentService;
import joat.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @PostMapping
    public ApiResponse<PostResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreatePostRequest req
    ) {
        return ApiResponse.ok(postService.createPost(userId, req));
    }

    @GetMapping
    public ApiResponse<CursorResponse<PostResponse>> feed(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(postService.getFeed(userId, cursor, limit));
    }

    @GetMapping("/explore")
    public ApiResponse<CursorResponse<PostResponse>> explore(
        @RequestParam(required = false) UUID cursor,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(postService.getExplore(cursor, limit));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> get(@PathVariable UUID postId) {
        return ApiResponse.ok(postService.getPost(postId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID postId) {
        postService.deletePost(postId, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/{postId}/like")
    public ApiResponse<Void> like(@AuthenticationPrincipal UUID userId, @PathVariable UUID postId) {
        postService.like(postId, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{postId}/like")
    public ApiResponse<Void> unlike(@AuthenticationPrincipal UUID userId, @PathVariable UUID postId) {
        postService.unlike(postId, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<List<CommentResponse>> comments(@PathVariable UUID postId) {
        return ApiResponse.ok(commentService.getComments(postId));
    }

    @PostMapping("/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId,
        @Valid @RequestBody CreateCommentRequest req
    ) {
        return ApiResponse.ok(commentService.createComment(postId, userId, req));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID postId,
        @PathVariable UUID commentId
    ) {
        commentService.deleteComment(postId, commentId, userId);
        return ApiResponse.ok();
    }
}
```

- [ ] **Step 8: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.feed.service.PostServiceTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 9: 커밋**

```bash
git add src/main/java/joat/feed/ src/test/java/joat/feed/
git commit -m "feat: feed 도메인 — 포스트/좋아요/댓글 API"
```

---

### Task 10: Tag 도메인 + Todo 인증 포스트 연결

**Files:**
- Create: `src/main/java/joat/tag/domain/{Tag, PostTag, PostTagId}.java`
- Create: `src/main/java/joat/tag/repository/{TagRepository, PostTagRepository}.java`
- Create: `src/main/java/joat/tag/dto/TagResponse.java`
- Create: `src/main/java/joat/tag/service/TagService.java`
- Create: `src/main/java/joat/tag/controller/TagController.java`
- Modify: `src/main/java/joat/todo/controller/TodoController.java` (certify 엔드포인트 추가)
- Test: `src/test/java/joat/tag/service/TagServiceTest.java`

- [ ] **Step 1: Tag 도메인 엔티티**

`src/main/java/joat/tag/domain/PostTagId.java`
```java
package joat.tag.domain;
import java.io.Serializable;
import java.util.UUID;
public record PostTagId(UUID postId, UUID tagId) implements Serializable {}
```

`src/main/java/joat/tag/domain/Tag.java`
```java
package joat.tag.domain;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "tags")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private int postCount;

    public static Tag of(String name) {
        Tag t = new Tag();
        t.name = name;
        return t;
    }

    public void incrementPostCount() { this.postCount++; }
}
```

`src/main/java/joat/tag/domain/PostTag.java`
```java
package joat.tag.domain;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "post_tags")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(PostTagId.class)
public class PostTag {
    @Id private UUID postId;
    @Id private UUID tagId;

    public static PostTag of(UUID postId, UUID tagId) {
        PostTag pt = new PostTag();
        pt.postId = postId;
        pt.tagId = tagId;
        return pt;
    }
}
```

- [ ] **Step 2: Repository + DTO**

`src/main/java/joat/tag/repository/TagRepository.java`
```java
package joat.tag.repository;
import joat.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
    List<Tag> findByNameStartingWithIgnoreCase(String prefix);
    List<Tag> findTop20ByOrderByPostCountDesc();
}
```

`src/main/java/joat/tag/repository/PostTagRepository.java`
```java
package joat.tag.repository;
import joat.tag.domain.PostTag;
import joat.tag.domain.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;
public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
    @Query("SELECT pt.postId FROM PostTag pt WHERE pt.tagId = :tagId")
    List<UUID> findPostIdsByTagId(UUID tagId);
}
```

`src/main/java/joat/tag/dto/TagResponse.java`
```java
package joat.tag.dto;
import joat.tag.domain.Tag;
import java.util.UUID;
public record TagResponse(UUID id, String name, int postCount) {
    public static TagResponse from(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getPostCount());
    }
}
```

- [ ] **Step 3: TagService 테스트**

`src/test/java/joat/tag/service/TagServiceTest.java`

```java
package joat.tag.service;

import joat.tag.domain.Tag;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @InjectMocks TagService tagService;
    @Mock TagRepository tagRepository;
    @Mock PostTagRepository postTagRepository;

    @Test
    void 트렌딩_태그_상위_20개를_반환한다() {
        given(tagRepository.findTop20ByOrderByPostCountDesc())
            .willReturn(List.of(Tag.of("갓생"), Tag.of("공부")));

        var result = tagService.getTrending();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("갓생");
    }
}
```

- [ ] **Step 4: TagService 구현**

`src/main/java/joat/tag/service/TagService.java`

```java
package joat.tag.service;

import joat.feed.domain.Post;
import joat.tag.domain.PostTag;
import joat.tag.domain.Tag;
import joat.tag.dto.TagResponse;
import joat.tag.repository.PostTagRepository;
import joat.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    @Transactional
    public void attachTags(Post post, List<String> tagNames) {
        for (String name : tagNames) {
            Tag tag = tagRepository.findByName(name.toLowerCase())
                .orElseGet(() -> tagRepository.save(Tag.of(name.toLowerCase())));
            postTagRepository.save(PostTag.of(post.getId(), tag.getId()));
            tag.incrementPostCount();
        }
    }

    public List<TagResponse> search(String query) {
        return tagRepository.findByNameStartingWithIgnoreCase(query)
            .stream().map(TagResponse::from).toList();
    }

    public List<TagResponse> getTrending() {
        return tagRepository.findTop20ByOrderByPostCountDesc()
            .stream().map(TagResponse::from).toList();
    }
}
```

- [ ] **Step 5: TagController 구현**

`src/main/java/joat/tag/controller/TagController.java`

```java
package joat.tag.controller;

import joat.common.response.ApiResponse;
import joat.tag.dto.TagResponse;
import joat.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> search(@RequestParam String q) {
        return ApiResponse.ok(tagService.search(q));
    }

    @GetMapping("/trending")
    public ApiResponse<List<TagResponse>> trending() {
        return ApiResponse.ok(tagService.getTrending());
    }
}
```

- [ ] **Step 6: TodoController에 certify 엔드포인트 추가**

`src/main/java/joat/todo/controller/TodoController.java` 에 메서드 추가:

```java
// 기존 import에 추가
import joat.feed.dto.PostResponse;
import joat.feed.service.PostService;
import joat.feed.dto.CreatePostRequest;
import java.util.List;

// 생성자 주입에 PostService 추가 후 메서드 추가:
@PostMapping("/{todoId}/certify")
public ApiResponse<PostResponse> certify(
    @AuthenticationPrincipal UUID userId,
    @PathVariable UUID todoId,
    @RequestBody CertifyRequest req
) {
    todoService.findTodo(todoId).validateOwner(userId);
    CreatePostRequest postReq = new CreatePostRequest(
        req.content(),
        req.imageUrls(),
        null,
        todoId
    );
    return ApiResponse.ok(postService.createPost(userId, postReq));
}
```

- [ ] **Step 7: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "joat.tag.service.TagServiceTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/joat/tag/ src/main/java/joat/todo/controller/ src/test/java/joat/tag/
git commit -m "feat: tag 도메인 — 해시태그 검색/트렌딩 + 투두 인증 포스트 API"
```

---

### Task 11: S3 이미지 업로드 API

**Files:**
- Create: `src/main/java/joat/common/s3/S3Service.java`
- Create: `src/main/java/joat/common/s3/ImageController.java`

- [ ] **Step 1: S3Service 구현**

`src/main/java/joat/common/s3/S3Service.java`

```java
package joat.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presigned-expiry}")
    private long expirySeconds;

    @Value("${aws.s3.region}")
    private String region;

    public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType, UUID userId) {
        String key = userId + "/" + UUID.randomUUID() + "/" + fileName;
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expirySeconds))
            .putObjectRequest(objectRequest)
            .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        String fileUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        return new PresignedUrlResponse(presigned.url().toString(), fileUrl);
    }

    public record PresignedUrlResponse(String presignedUrl, String fileUrl) {}
}
```

- [ ] **Step 2: S3 Bean 설정**

`src/main/java/joat/common/s3/S3Config.java`

```java
package joat.common.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}") private String region;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .build();
    }
}
```

- [ ] **Step 3: ImageController 구현**

`src/main/java/joat/common/s3/ImageController.java`

```java
package joat.common.s3;

import joat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    public ApiResponse<S3Service.PresignedUrlResponse> presignedUrl(
        @AuthenticationPrincipal UUID userId,
        @RequestParam String fileName,
        @RequestParam String contentType
    ) {
        return ApiResponse.ok(s3Service.generatePresignedUrl(fileName, contentType, userId));
    }
}
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/joat/common/s3/
git commit -m "feat: S3 Presigned URL 이미지 업로드 API"
```

---

### Task 12: 전체 빌드 + 연기/통합 검증

- [ ] **Step 1: 전체 테스트 실행**

```bash
./gradlew test 2>&1 | tail -15
```

Expected: 모든 테스트 PASS, `BUILD SUCCESSFUL`

- [ ] **Step 2: 로컬 서버 기동 (마이그레이션 실행 포함)**

```bash
docker-compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Expected: `Started JoatApplication` 로그 출력, Flyway 마이그레이션 완료

- [ ] **Step 3: 핵심 API 스모크 테스트**

```bash
# 탐색 피드 (인증 불필요)
curl http://localhost:8080/api/posts/explore
# 응답: {"success":true,"data":{"data":[],"nextCursor":null,"hasNext":false}}

# 트렌딩 태그 (인증 불필요)
curl http://localhost:8080/api/tags/trending
# 응답: {"success":true,"data":[]}

# 인증 필요 API (토큰 없이) → 403
curl http://localhost:8080/api/users/me
# 응답: 403
```

- [ ] **Step 4: 최종 커밋**

```bash
git add -A
git commit -m "feat: 갓생 커뮤니티 앱 백엔드 MVP 구현 완료"
```

---

---

### Task 0 (선행): Kafka + Redis 인프라 설정

> Task 1보다 먼저 실행. docker-compose, 의존성, Kafka 토픽, Redis 고도화 설정.

**Files:**
- Modify: `docker-compose.yml`
- Modify: `build.gradle` (spring-kafka 추가)
- Create: `src/main/java/joat/common/kafka/KafkaConfig.java`
- Create: `src/main/java/joat/common/kafka/KafkaTopics.java`
- Create: `src/main/java/joat/common/kafka/event/PostCreatedEvent.java`
- Create: `src/main/java/joat/common/kafka/PostEventProducer.java`
- Create: `src/main/java/joat/common/redis/RedisConfig.java`
- Modify: `src/main/resources/application.yaml` (Kafka + Redis 설정 추가)
- Modify: `src/main/resources/application-local.yaml`

**Kafka 역할:** PostService가 포스트 생성 시 `post.created` 이벤트를 발행 → TagService Kafka Consumer가 비동기로 해시태그 처리. 도메인 간 직접 의존 제거.

**Redis 역할:**
- `auth:refresh:{userId}` — refresh token (기존)
- `tags:trending` — Sorted Set (tagName → postCount score) — DB 쿼리 없이 트렌딩 태그 조회
- `feed:home:{userId}` — 홈 피드 캐시 (TTL 3분)

- [ ] **Step 1: docker-compose.yml에 Kafka + Zookeeper 추가**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: joat
      POSTGRES_USER: joat
      POSTGRES_PASSWORD: joat
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

volumes:
  postgres_data:
```

- [ ] **Step 2: build.gradle에 spring-kafka 추가**

기존 `dependencies` 블록에 추가:
```groovy
implementation 'org.springframework.kafka:spring-kafka'
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

- [ ] **Step 3: application.yaml Kafka + Redis 설정 추가**

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: joat-app
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "joat.common.kafka.event"
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

- [ ] **Step 4: KafkaTopics 상수 클래스**

`src/main/java/joat/common/kafka/KafkaTopics.java`

```java
package joat.common.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String POST_CREATED = "post.created";
}
```

- [ ] **Step 5: PostCreatedEvent**

`src/main/java/joat/common/kafka/event/PostCreatedEvent.java`

```java
package joat.common.kafka.event;

import java.util.List;
import java.util.UUID;

public record PostCreatedEvent(
    UUID postId,
    UUID userId,
    List<String> tagNames
) {}
```

- [ ] **Step 6: KafkaConfig**

`src/main/java/joat/common/kafka/KafkaConfig.java`

```java
package joat.common.kafka;

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

- [ ] **Step 7: PostEventProducer**

`src/main/java/joat/common/kafka/PostEventProducer.java`

```java
package joat.common.kafka;

import joat.common.kafka.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventProducer {

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    public void publishPostCreated(PostCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.POST_CREATED, event.postId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) log.error("Failed to publish PostCreatedEvent: {}", ex.getMessage());
            });
    }
}
```

- [ ] **Step 8: RedisConfig (ObjectMapper 기반 직렬화)**

`src/main/java/joat/common/redis/RedisConfig.java`

```java
package joat.common.redis;

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

- [ ] **Step 9: docker-compose up 및 연결 확인**

```bash
docker-compose up -d
# Kafka 브로커 준비 대기 (약 15초)
docker-compose logs kafka | grep "started (kafka.server.KafkaServer)"
# 기대: [KafkaServer id=1] started
```

- [ ] **Step 10: 빌드 확인**

```bash
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: 커밋**

```bash
git add docker-compose.yml build.gradle src/main/java/joat/common/kafka/ src/main/java/joat/common/redis/ src/main/resources/application*.yaml
git commit -m "feat: Kafka + Redis 인프라 설정 (토픽, 프로듀서, RedisConfig)"
```

**Task 10 업데이트 (Tag + Kafka Consumer):**

Tag 도메인에서 `TagService`를 직접 호출하는 대신 Kafka Consumer로 비동기 처리.

추가 파일:
- Create: `src/main/java/joat/tag/kafka/TagEventConsumer.java`
- Modify: `src/main/java/joat/feed/service/PostService.java` (TagService 의존 → PostEventProducer)

`src/main/java/joat/tag/kafka/TagEventConsumer.java`

```java
package joat.tag.kafka;

import joat.common.kafka.KafkaTopics;
import joat.common.kafka.event.PostCreatedEvent;
import joat.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagEventConsumer {

    private final TagService tagService;

    @KafkaListener(topics = KafkaTopics.POST_CREATED, groupId = "tag-service")
    public void onPostCreated(PostCreatedEvent event) {
        if (event.tagNames() == null || event.tagNames().isEmpty()) return;
        try {
            tagService.processTags(event.postId(), event.tagNames());
        } catch (Exception e) {
            log.error("Tag processing failed for postId={}: {}", event.postId(), e.getMessage());
        }
    }
}
```

`TagService.processTags()` 추가:

```java
@Transactional
public void processTags(UUID postId, List<String> tagNames) {
    for (String name : tagNames) {
        String normalized = name.toLowerCase();
        Tag tag = tagRepository.findByName(normalized)
            .orElseGet(() -> tagRepository.save(Tag.of(normalized)));
        postTagRepository.save(PostTag.of(postId, tag.getId()));
        tag.incrementPostCount();
        // Redis Sorted Set 업데이트
        redisTemplate.opsForZSet().incrementScore("tags:trending", normalized, 1);
    }
}
```

`TagService.getTrending()` — Redis 우선 조회:

```java
public List<TagResponse> getTrending() {
    Set<Object> cached = redisTemplate.opsForZSet()
        .reverseRange("tags:trending", 0, 19);
    if (cached != null && !cached.isEmpty()) {
        return cached.stream()
            .map(name -> tagRepository.findByName(name.toString())
                .map(TagResponse::from).orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }
    return tagRepository.findTop20ByOrderByPostCountDesc()
        .stream().map(TagResponse::from).toList();
}
```

`PostService`에서 TagService 직접 의존 제거 → PostEventProducer 사용:

```java
// 기존: tagService.attachTags(saved, req.tagNames());
// 변경:
if (req.tagNames() != null && !req.tagNames().isEmpty()) {
    eventProducer.publishPostCreated(
        new PostCreatedEvent(saved.getId(), userId, req.tagNames()));
}
```

---

## 스펙 커버리지 체크

| 스펙 항목 | 구현 Task |
|---|---|
| 카카오/구글 OAuth | Task 6 |
| JWT access/refresh | Task 5, 6 |
| 유저 프로필/팔로우 | Task 7 |
| 투두 CRUD + 항목 체크 | Task 8 |
| 투두 인증 포스트 | Task 10 (certify) |
| 자유 포스트 CRUD | Task 9 |
| 팔로잉 기반 피드 | Task 9 |
| 탐색 피드 | Task 9 |
| 좋아요 | Task 9 |
| 댓글/대댓글 | Task 9 |
| 해시태그 검색/트렌딩 | Task 10 |
| S3 이미지 업로드 | Task 11 |
| DB 스키마 (Flyway) | Task 3 |
| Redis refresh token | Task 6 |
