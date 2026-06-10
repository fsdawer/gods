---
name: infra-skill
description: AWS 인프라, Docker, CI/CD, 환경 설정 작업 시 사용.
---

# 인프라 작업 체크리스트

## 작업 시작 전

- [ ] `.claude/docs/infra-docs.md` 읽기 (환경 변수 전체 목록, 설정 파일 위치)
- [ ] 모든 `@Configuration` 클래스는 `joat.global.config` 패키지에 작성

## 환경 변수 체크리스트

- [ ] 필수 환경 변수 누락 없는가? (`DB_URL`, `JWT_SECRET`, `AWS_S3_BUCKET` 등)
- [ ] 환경별 분리 확인 (local/prod 프로파일)
- [ ] 비밀 값이 코드에 하드코딩 없는가?

## application.yaml 규칙

- [ ] `spring.jpa.hibernate.ddl-auto=validate` (create/update 금지)
- [ ] `spring.flyway.enabled=true`
- [ ] 환경 변수는 `${ENV_VAR}` 형식으로 참조

## Docker 체크리스트

- [ ] `Dockerfile` 베이스 이미지: `eclipse-temurin:17-jre-alpine`
- [ ] 로컬 개발용 `docker-compose.yml`에 PostgreSQL + Redis 포함

## 배포 전 확인

- [ ] `./gradlew test` 전체 통과
- [ ] 환경 변수 주입 확인
- [ ] Flyway 마이그레이션 순서 확인
