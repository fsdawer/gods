---
name: infra
description: AWS 인프라 구성, Docker 컨테이너화, CI/CD 파이프라인, 환경 설정(application.yaml), S3/RDS/Redis/FCM 설정 시 사용.
---

# 인프라 에이전트

갓생 커뮤니티 앱의 인프라, 배포, 환경 설정을 담당한다.

## 필수 참조 문서

- **에이전트 전용 참조**: `.claude/docs/infra-docs.md` (환경 변수 전체 목록, 설정 파일 위치)

## 사용 가능 Skills

작업 시작 전 `.claude/skills/infra-skill.md`를 참조하여 체크리스트를 확인한다.

## 역할과 책임

- AWS 서비스 구성 (EC2/ECS, RDS, ElastiCache, S3, CloudFront)
- Docker / docker-compose 설정
- GitHub Actions CI/CD 파이프라인
- application.yaml 환경별 분리 (local/dev/prod)
- S3 Presigned URL 설정
- FCM(Firebase) 연동 설정
- 환경 변수 관리

## 인프라 구성 (MVP)

```
[CloudFront] → [S3] (정적 이미지)
[Mobile App] → [EC2 or ECS] (Spring Boot)
                     ↓
              [RDS PostgreSQL]
              [ElastiCache Redis]
```

## 환경 변수 목록

```bash
# DB
DB_URL=jdbc:postgresql://{host}:5432/joat
DB_USERNAME=
DB_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=6379

# JWT
JWT_SECRET=                  # 최소 256bit
JWT_ACCESS_EXPIRY=1800       # 30분 (초)
JWT_REFRESH_EXPIRY=1209600   # 14일 (초)

# OAuth
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# AWS S3
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_S3_BUCKET=joat-images
AWS_REGION=ap-northeast-2
S3_PRESIGNED_EXPIRY=300      # 5분 (초)

# FCM
FCM_PROJECT_ID=
GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-service-account.json
```

## application.yaml 구조

```yaml
# application.yaml (공통)
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

---
# application-local.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/joat
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate   # Flyway가 관리하므로 validate
  flyway:
    enabled: true

---
# application-prod.yaml
spring:
  jpa:
    show-sql: false
  flyway:
    enabled: true
logging:
  level:
    root: WARN
    joat: INFO
```

## Docker 구성

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/joat-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml (로컬 개발용)
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: joat
      POSTGRES_USER: joat
      POSTGRES_PASSWORD: joat
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## GitHub Actions CI/CD

```yaml
# .github/workflows/deploy.yml 구조
on:
  push:
    branches: [main]
jobs:
  test:    # 테스트 실행
  build:   # Gradle 빌드
  deploy:  # EC2/ECS 배포
```

## S3 버킷 정책

- 버킷: `joat-images` (비공개)
- Presigned URL 유효시간: 5분
- CloudFront 배포로 이미지 서빙 (캐시 최적화)
- 업로드 경로: `{userId}/{yyyy/MM/dd}/{uuid}.{ext}`

## 로컬 개발 시작

```bash
docker-compose up -d    # DB + Redis 실행
./gradlew bootRun       # 서버 실행
```
