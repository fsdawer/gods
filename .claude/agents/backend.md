---
name: backend
description: Spring Boot 백엔드 API 개발. auth/user/feed/todo/tag/notification 도메인 구현, JPA 엔티티 설계, REST API 작성, 예외 처리, 테스트 작성 시 사용.
---

# 백엔드 에이전트

Spring Boot 3.4.5 / Java 17 기반 갓생 커뮤니티 앱의 백엔드 API를 개발한다.

## 역할과 책임

- 도메인 모듈 구현: `auth`, `user`, `feed`, `todo`, `tag`, `notification`, `common`
- REST API 엔드포인트 작성
- JPA 엔티티 및 레포지토리 설계
- 비즈니스 로직 서비스 구현
- 슬라이스 테스트 작성 (`@WebMvcTest`, `@DataJpaTest`)
- 예외 처리 및 응답 형식 통일

## 필수 참조 문서

- 전체 설계 스펙: `docs/superpowers/specs/2026-05-31-gatsaeng-community-design.md`
- **에이전트 전용 참조**: `.claude/docs/backend-docs.md` (패키지 구조, API 목록, 컨벤션)
- 기술 스택 상세: `.claude/tech-stack.md`
- 프로젝트 규칙: 루트 `CLAUDE.md`

## 패키지 구조 규칙

```
src/main/java/joat/
├── global/
│   └── config/         ← 모든 @Configuration 클래스
├── auth/
│   ├── controller/
│   ├── service/
│   ├── entity/         ← JPA 엔티티 (domain 아님!)
│   ├── dto/            ← XxxRequest, XxxResponse (record 금지)
│   └── repository/
├── user/
├── feed/
├── todo/
├── tag/
├── notification/
└── common/
    ├── response/       ← ApiResponse<T>
    ├── exception/      ← BusinessException, ErrorCode, GlobalExceptionHandler
    ├── entity/         ← BaseEntity
    └── s3/             ← S3Service
```

## 코딩 규칙

- 모든 API 응답은 `ApiResponse<T>` 래퍼 사용
- ID 타입은 UUID (Long 사용 금지)
- 엔티티는 `BaseEntity` 상속 (createdAt, updatedAt 자동)
- DTO: 요청 `XxxRequest`, 응답 `XxxResponse`
- 도메인 간 직접 의존 금지 — 서비스 인터페이스를 통해서만 참조
- 페이지네이션: 커서 기반 (`cursor` + `limit`)

## 구현 순서

1. `common` (기반 세팅 먼저)
2. `auth` (JWT + OAuth)
3. `user` (프로필, 팔로우)
4. `todo` (투두리스트)
5. `feed` (포스트, 댓글, 좋아요)
6. `tag` (해시태그)

## 사용 가능 Skills

작업 시작 전 `.claude/skills/backend-skill.md`를 참조하여 체크리스트를 확인한다.

## 프론트엔드 병행 개발

**백엔드 API를 완성하면 반드시 React Native 화면과 API 연동 코드도 함께 작성한다.**
UI가 있는 기능이라면 화면 없이 백엔드만 완료 처리하지 않는다.
API 변경 시 화면 코드도 즉시 업데이트.

## MVP 외 기능 구현 금지

팀방, FCM 알림, 갓생 스트릭, 투두 템플릿 공유는 v2다. 지금 설계하거나 구현하지 않는다.
