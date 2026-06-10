# 백엔드 개발 참조 문서

## 패키지 구조 (최신)

```
src/main/java/joat/
├── global/
│   └── config/              ← 모든 @Configuration 클래스 (Security, S3, Redis, Kafka, JPA)
├── auth/
│   ├── client/              ← KakaoOAuthClient, GoogleOAuthClient
│   ├── controller/          ← AuthController
│   ├── dto/                 ← TokenResponse, OAuthLoginRequest (일반 class, record 아님)
│   ├── jwt/                 ← JwtUtil, JwtFilter
│   └── service/             ← AuthService(인터페이스) + AuthServiceImpl
├── user/
│   ├── controller/
│   ├── dto/                 ← UserProfileResponse, UpdateProfileRequest, FollowListResponse
│   ├── entity/              ← User, Follow, FollowId, OAuthProvider (domain 아님!)
│   ├── repository/
│   └── service/
├── feed/
│   ├── controller/
│   ├── dto/                 ← PostResponse, CommentResponse, CursorResponse, CreatePostRequest
│   ├── entity/              ← Post, Comment, Like, LikeId, PostType
│   ├── repository/
│   └── service/
├── todo/
│   ├── controller/
│   ├── dto/                 ← TodoResponse, TodoItemResponse (분리됨), CreateTodoRequest
│   ├── entity/              ← Todo, TodoItem
│   ├── repository/
│   └── service/
├── tag/
│   ├── controller/
│   ├── dto/                 ← TagResponse
│   ├── entity/              ← Tag, PostTag, PostTagId
│   ├── kafka/               ← TagEventConsumer
│   ├── repository/
│   └── service/
└── common/
    ├── entity/              ← BaseEntity
    ├── exception/           ← GlobalExceptionHandler, BusinessException, ErrorCode
    ├── kafka/               ← PostEventProducer, KafkaTopics, event/PostCreatedEvent
    ├── response/            ← ApiResponse<T>
    └── s3/                  ← S3Service, ImageController
```

## 코딩 규칙 요약

- **DTO는 record 아닌 일반 class** — `@Getter @NoArgsConstructor @AllArgsConstructor`
- **JPA 복합 PK** — `@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode`
- **엔티티 패키지** — `*/entity/` (domain 아님)
- **설정 패키지** — `joat.global.config`
- **서비스 레이어** — 인터페이스(XxxService) + 구현체(XxxServiceImpl)
- **응답** — 모든 API는 `ApiResponse<T>` 래퍼
- **ID** — UUID (Long 금지)
- **페이지네이션** — 커서 기반 (`CursorResponse<T>`)

## API 엔드포인트 목록

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | /api/auth/oauth/kakao | X | 카카오 로그인 |
| POST | /api/auth/oauth/google | X | 구글 로그인 |
| POST | /api/auth/token/refresh | X | 토큰 재발급 |
| POST | /api/auth/logout | O | 로그아웃 |
| GET | /api/users/me | O | 내 프로필 조회 |
| PATCH | /api/users/me | O | 프로필 수정 |
| POST | /api/users/{id}/follow | O | 팔로우 |
| DELETE | /api/users/{id}/follow | O | 언팔로우 |
| GET | /api/posts | O | 홈 피드 |
| GET | /api/posts/explore | X | 탐색 피드 |
| POST | /api/posts | O | 포스트 생성 |
| DELETE | /api/posts/{id} | O | 포스트 삭제 |
| POST | /api/posts/{id}/like | O | 좋아요 |
| DELETE | /api/posts/{id}/like | O | 좋아요 취소 |
| GET | /api/posts/{id}/comments | O | 댓글 조회 |
| POST | /api/posts/{id}/comments | O | 댓글 작성 |
| DELETE | /api/posts/{id}/comments/{cid} | O | 댓글 삭제 |
| GET | /api/todos | O | 내 투두 목록 |
| POST | /api/todos | O | 투두 생성 |
| DELETE | /api/todos/{id} | O | 투두 삭제 |
| PATCH | /api/todos/{id}/items/{iid} | O | 항목 체크 |
| POST | /api/todos/{id}/certify | O | 투두 인증 포스트 |
| GET | /api/tags/search | X | 태그 검색 |
| GET | /api/tags/trending | X | 트렌딩 태그 |
| POST | /api/images/presigned-url | O | S3 Presigned URL 발급 |

## 예외 처리 규칙

- 모든 비즈니스 예외 → `BusinessException(ErrorCode.XXX)`
- `GlobalExceptionHandler`가 `ApiResponse.fail()` 형식으로 응답
- HTTP 상태코드는 `ErrorCode`에 정의된 값 사용
