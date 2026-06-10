---
name: db-skill
description: DB 스키마 설계, 마이그레이션 작성, 엔티티 최적화 시 사용. 직접 DB 실행 금지.
---

# DB 작업 체크리스트

## 절대 금지 (시작 전 재확인)

- DB 클라이언트(psql 등) 직접 실행 금지
- 이미 적용된 Flyway 파일 수정 금지
- `ddl-auto=create/update` 설정 금지
- **파일 작성만. 실행은 사람이 한다.**

## 작업 시작 전

- [ ] `.claude/docs/db-docs.md` 읽기 (스키마, 인덱스, Redis 전략)
- [ ] 기존 Flyway 파일 버전 확인 (`src/main/resources/db/migration/`)

## 엔티티 작성 체크리스트

- [ ] 엔티티는 `{domain}/entity/` 패키지에 작성
- [ ] `BaseEntity` 상속 확인 (createdAt, updatedAt 자동 관리)
- [ ] ID 타입은 UUID, `@GeneratedValue(strategy = GenerationType.UUID)`
- [ ] `@OneToMany`는 `LAZY` 로딩 기본
- [ ] 복합 PK는 `@IdClass` + `@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode implements Serializable`

## Flyway 마이그레이션 작성 체크리스트

- [ ] 파일명: `V{N}__{설명}.sql` (언더스코어 두 개)
- [ ] 위치: `src/main/resources/db/migration/`
- [ ] UUID 기본값: `DEFAULT gen_random_uuid()`
- [ ] 타임스탬프: `DEFAULT now()`
- [ ] 인덱스 포함 여부 확인

## 성능 체크리스트

- [ ] 피드 조회 N+1 없는가? (`@EntityGraph` 또는 fetch join)
- [ ] `like_count`, `comment_count` COUNT 쿼리 없는가? (카운터 캐시 패턴)
- [ ] 조회 조건 컬럼에 인덱스 있는가?
- [ ] Redis 캐시 대상인지 확인 (`tags:trending`, `auth:refresh:{userId}`)
