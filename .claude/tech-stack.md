# 기술 스택 참조 문서

## 백엔드 의존성 추가 예정 (build.gradle)

```groovy
dependencies {
    // Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // JPA + DB
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Security + OAuth2
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // AWS S3
    implementation 'software.amazon.awssdk:s3:2.25.0'

    // FCM
    implementation 'com.google.firebase:firebase-admin:9.2.0'

    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

---

## JWT 전략

- **Access Token:** 30분 만료, 모든 API 요청 헤더에 포함
- **Refresh Token:** 14일 만료, Redis에 저장 (userId → refreshToken)
- **재발급:** `POST /api/auth/token/refresh` — refresh token 검증 후 새 access token 발급
- **로그아웃:** Redis에서 refresh token 삭제

---

## OAuth2 플로우 (카카오/구글)

```
앱 → 카카오/구글 SDK → 액세스 토큰 획득
앱 → POST /api/auth/oauth/kakao { accessToken }
서버 → 카카오 API로 유저 정보 조회
서버 → users 테이블 upsert (oauth_provider + oauth_id 기준)
서버 → JWT(access + refresh) 응답
앱 → SecureStorage 저장
```

---

## S3 이미지 업로드 플로우

```
앱 → POST /api/images/presigned-url { fileName, contentType }
서버 → S3 Presigned PUT URL 생성 (5분 유효)
서버 → { presignedUrl, fileUrl } 응답
앱 → presignedUrl 에 직접 PUT 업로드
포스트 작성 시 fileUrl 을 image_urls 에 포함
```

---

## 피드 페이지네이션

커서 기반 페이지네이션 사용:
```
GET /api/posts?cursor={lastPostId}&limit=20
응답: { data: [...], nextCursor: "uuid", hasNext: true }
```
- 시간순 정렬 (created_at DESC)
- Redis로 인기 포스트(좋아요 기준) 캐싱 — TTL 10분

---

## 공통 응답 형식

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "USER_NOT_FOUND", "message": "유저를 찾을 수 없습니다." } }
```
