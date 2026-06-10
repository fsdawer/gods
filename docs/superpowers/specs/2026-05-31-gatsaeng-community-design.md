# 갓생 커뮤니티 앱 설계 스펙

**날짜:** 2026-05-31  
**프로젝트:** joat (Java On A Task)  
**상태:** 확정

---

## 1. 개요

갓생을 사는 직장인·취준생을 위한 모바일 커뮤니티 앱. 인스타그램식 피드로 갓생 루틴을 공유하고, 투두리스트로 계획을 관리하며, 비슷한 사람들과 동기를 나누는 플랫폼.

**레퍼런스:** 셋로그(setlog) 스타일 갓생 인증 커뮤니티

---

## 2. 확정 스펙 요약

| 항목 | 선택 |
|---|---|
| 플랫폼 | 모바일 앱 우선 (React Native) |
| 백엔드 | Spring Boot 3.4.5 / Java 17 |
| 아키텍처 | 모듈형 모놀리스 |
| 인증 | 카카오 + 구글 OAuth → JWT |
| MVP 핵심 기능 | 피드 + 해시태그 + 투두리스트 공유 |
| 게시물 타입 | 자유 포스트 + 투두 완료 인증 포스트 |
| DB | PostgreSQL + Redis(캐시) |
| 이미지 저장 | AWS S3 (Presigned URL 직접 업로드) |
| 푸시 알림 | FCM |
| v2 예정 | 팀방(공유 스터디방) |

---

## 3. 시스템 아키텍처

```
[React Native App]
       ↓ REST API (JWT)
[Spring Boot — 모듈형 모놀리스]
  ├── auth       : OAuth, JWT 발급/갱신
  ├── user       : 프로필, 팔로우
  ├── feed       : 포스트 CRUD, 좋아요, 댓글
  ├── todo       : 투두리스트, 완료 인증
  ├── tag        : 해시태그
  └── notification: FCM 푸시
       ↓
  PostgreSQL (메인 DB)
  Redis (캐시, 피드 인기순)
  AWS S3 (이미지)
  FCM (푸시 알림)
```

---

## 4. 데이터 모델

### users
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID PK | |
| nickname | VARCHAR | |
| profile_image_url | TEXT | S3 URL |
| bio | TEXT | |
| oauth_provider | ENUM | kakao / google |
| oauth_id | VARCHAR | |
| created_at | TIMESTAMP | |

### posts
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK | → users |
| type | ENUM | free / todo_cert |
| content | TEXT | |
| image_urls | TEXT[] | S3 URL 배열 |
| todo_id | UUID FK nullable | → todos (인증 포스트만) |
| like_count | INT | |
| comment_count | INT | |
| created_at | TIMESTAMP | |

### todos
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK | → users |
| title | VARCHAR | |
| is_public | BOOLEAN | 공개 여부 |
| date | DATE | |
| created_at | TIMESTAMP | |

### todo_items
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID PK | |
| todo_id | UUID FK | → todos |
| content | VARCHAR | |
| is_done | BOOLEAN | |
| order_idx | INT | 순서 |

### comments
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID PK | |
| post_id | UUID FK | → posts |
| user_id | UUID FK | → users |
| parent_id | UUID FK nullable | → comments (대댓글) |
| content | TEXT | |
| created_at | TIMESTAMP | |

### 관계 테이블
- `tags`: id, name, post_count
- `post_tags`: post_id, tag_id
- `follows`: follower_id, following_id
- `likes`: user_id, post_id

---

## 5. API 엔드포인트

### Auth — `/api/auth`
```
POST   /oauth/kakao
POST   /oauth/google
POST   /token/refresh
DELETE /logout
```

### User — `/api/users`
```
GET    /me
PATCH  /me
GET    /{userId}
POST   /{userId}/follow
DELETE /{userId}/follow
GET    /{userId}/followers
GET    /{userId}/following
```

### Feed — `/api/posts`
```
GET    /              # 팔로잉 기반 홈 피드
GET    /explore       # 전체 탐색 피드
POST   /
GET    /{postId}
DELETE /{postId}
POST   /{postId}/like
DELETE /{postId}/like
GET    /{postId}/comments
POST   /{postId}/comments
DELETE /{postId}/comments/{commentId}
```

### Todo — `/api/todos`
```
GET    /                       # ?date=2026-05-31
POST   /
PATCH  /{todoId}
DELETE /{todoId}
PATCH  /{todoId}/items/{itemId}   # 체크/해제
POST   /{todoId}/certify          # 투두 인증 포스트 생성
GET    /users/{userId}            # 타인의 공개 투두 목록
```

### Tag — `/api/tags`
```
GET    /search         # ?q=개발
GET    /trending
GET    /{tagName}/posts
```

### Image — `/api/images`
```
POST   /presigned-url  # S3 직접 업로드용 URL 발급
```

---

## 6. 앱 화면 구조 (React Native)

Bottom Tab 5개:

| 탭 | 스크린 |
|---|---|
| 🏠 홈 | 팔로잉 피드 → 포스트 상세 → 댓글 / 유저 프로필 |
| 🔍 탐색 | 전체 피드, 인기 포스트, 해시태그 검색, 태그별 목록 |
| ＋ 올리기 | 자유 포스트 작성 모달 / 투두 인증 포스트 작성 모달 |
| ✅ 투두 | 오늘 투두, 날짜 전환, 완료 → 인증 바로 올리기 |
| 👤 프로필 | 내 포스트, 공개 투두, 팔로워/팔로잉, 프로필 편집 |

---

## 7. 주요 흐름

### 투두 인증 포스트 생성
1. 투두 탭에서 항목 체크 완료
2. "인증하기" 버튼 → 인증 포스트 작성 모달
3. `POST /api/todos/{todoId}/certify` 호출
4. 서버에서 `posts` 레코드(type=todo_cert, todo_id 연결) 생성
5. 홈 피드에 노출

### 이미지 업로드
1. 앱 → `POST /api/images/presigned-url` (파일명, MIME 타입 전달)
2. 서버 → S3 Presigned PUT URL 반환
3. 앱 → S3에 직접 PUT 업로드
4. 포스트 생성 시 S3 URL을 `image_urls`에 포함

### OAuth 로그인
1. 앱에서 카카오/구글 SDK로 액세스 토큰 획득
2. `POST /api/auth/oauth/{provider}` 에 토큰 전달
3. 서버에서 유저 조회/생성 후 JWT(access + refresh) 반환
4. 앱에서 SecureStorage에 JWT 저장

---

## 8. v2 로드맵

- **팀방:** 여러 유저가 같은 투두/피드를 공유하는 그룹 공간
- **알림:** 좋아요, 댓글, 팔로우 FCM 푸시
- **갓생 스트릭:** 연속 인증일 배지
- **투두 템플릿 공유:** 다른 유저 투두 복사해서 사용
