# CLAUDE.md — joat

## 프로젝트

취준생·직장인이 갓생을 살도록 돕는 공부 공유 커뮤니티 앱.
피드 공유 / 투두리스트 공개 / 카테고리 탐색 / 팀방(채팅) 4가지가 핵심.
"이게 취준생이 갓생 살도록 돕는가?" — 기능 추가 시 이 기준으로 판단.

- **백엔드:** Spring Boot 3.4.5 / Java 17 · **앱:** React Native (별도 레포)
- 상세 스펙 → `docs/superpowers/specs/2026-05-31-gatsaeng-community-design.md`
- 기술 스택 → `.claude/tech-stack.md` · 에이전트 → `.claude/agents/`

---

## 작업 규칙 (예외 없음)

| # | 규칙 |
|---|---|
| 1 | **워크트리 우선** — 모든 구현은 `git worktree add ../joat-feat-<name> -b feat/<name>` 후 진행. main 직접 커밋 금지. |
| 2 | **테스트 통과 전 커밋 금지** — 커밋 전 `./gradlew test` 전체 통과 필수. 테스트 없는 코드는 테스트 먼저. |
| 3 | **핀포인트 수정** — 오류 수정 시 그 부분만. 연쇄 수정으로 오류 숨기기 금지. |
| 4 | **명시 임포트** — FQCN(`java.util.Map`, `java.util.Optional` 등) 필드·파라미터·변수 선언에 사용 금지. 반드시 파일 상단에 `import java.util.Optional;` 형태로 추가 후 단순명으로 사용. |
| 5 | **백엔드+프론트 동시** — API 만들면 RN 화면도 함께. 순서: 서비스 → 엔드포인트 → RN 화면 → 연동. |
| 6 | **주석 동기화** — 메서드·필드를 수정하면 해당 Javadoc/인라인 주석도 반드시 함께 수정. 시그니처(파라미터·반환값)가 바뀌면 `@param`·`@return`·플로우 설명도 갱신. 새 메서드 추가 시 주석 작성 필수. |

---

## DB 접근 금지

AI는 DB를 직접 조작하지 않는다.
- **금지:** DB 클라이언트 CLI 실행, DML/DDL 직접 실행, 기존 Flyway 파일 수정, `ddl-auto=create/update`
- **허용:** 새 Flyway `.sql` 파일 작성(실행은 사람이), JPA 엔티티·JPQL·QueryDSL 코드 작성

---

## 서비스 레이어

```
{domain}/service/
├── XxxService.java      ← 인터페이스
└── XxxServiceImpl.java  ← @Service 구현체
```

- Controller·타 Service는 **인터페이스 타입**으로만 주입 (`private final XxxService`)
- 도메인 간 참조는 Service 인터페이스 통해서만 (직접 의존 금지)

---

## 코딩 컨벤션

- **응답:** `ApiResponse<T>` 래퍼 사용
- **예외:** `GlobalExceptionHandler` + `BusinessException` + `ErrorCode` enum
- **ID:** UUID v7 (`UuidCreator.getTimeOrderedEpoch()`) — Long 금지
- **엔티티:** `BaseEntity` 상속 / **DTO:** `XxxRequest`, `XxxResponse`
- **페이지네이션:** 커서 기반 (`?cursor=xxx&limit=20`)
- **이미지:** Presigned URL로 앱에서 S3 직접 PUT (서버 경유 금지)
- **테스트:** `@WebMvcTest`, `@DataJpaTest` 슬라이스 테스트 우선

---

## MVP vs v2

**MVP (구현 대상):** auth / user / feed / todo / tag / common(S3, 예외, 응답 래퍼)

**v2 (지금 구현·설계 금지):** 팀방 채팅, 갓생 스트릭, 투두 템플릿 공유, FCM 알림
