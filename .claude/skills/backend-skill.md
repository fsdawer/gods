---
name: backend-skill
description: 백엔드 코드 작성 전 필수 체크리스트. API 작성, 엔티티 설계, 서비스 구현 시 사용.
---

# 백엔드 작업 체크리스트

## 작업 시작 전

- [ ] `.claude/docs/backend-docs.md` 읽기 (패키지 구조, API 목록 확인)
- [ ] 작업 대상 도메인의 기존 코드 구조 파악 (controller/service/entity/repository)
- [ ] `CLAUDE.md` 규칙 확인 (워크트리 원칙, 핀포인트 수정 원칙)

## 파일 생성 규칙

- 엔티티: `{domain}/entity/` (domain 폴더 사용 금지)
- DTO: record 금지 → `@Getter @NoArgsConstructor @AllArgsConstructor`
- 설정: `joat.global.config`
- 복합 PK: `@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode implements Serializable`

## 서비스 구현 체크리스트

- [ ] 인터페이스 (`XxxService.java`) 먼저 작성
- [ ] 구현체 (`XxxServiceImpl.java`) 작성 — `@Service implements XxxService`
- [ ] Controller는 인터페이스 타입으로 주입 (`private final XxxService xxxService`)
- [ ] 각 메서드에 플로우/입력/호출/반환 Javadoc 작성

## API 엔드포인트 작성 체크리스트

- [ ] 응답은 `ApiResponse<T>` 래퍼 사용
- [ ] 비즈니스 예외는 `BusinessException(ErrorCode.XXX)` 사용
- [ ] SecurityConfig에 인증 필요 여부 반영
- [ ] 페이지네이션이 필요하면 커서 기반 (`CursorResponse<T>`)

## 커밋 전 확인

- [ ] `./gradlew compileJava` 성공
- [ ] `./gradlew test` 전체 통과
- [ ] 불필요한 import 없음
- [ ] 프론트엔드 화면/API 연동 코드도 함께 작성됐는가?

## 백엔드+프론트 동시 개발 원칙

**API 완성 후 반드시 React Native 화면과 API 연동 코드도 함께 구현한다.**
화면에 버튼/UI가 있는 API라면 화면 없이 백엔드만 완료 처리하지 않는다.
