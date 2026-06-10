# 인프라 참조 문서

## 환경 변수 전체 목록

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
KAKAO_USER_INFO_URL=https://kapi.kakao.com/v2/user/me
GOOGLE_USER_INFO_URL=https://www.googleapis.com/oauth2/v3/userinfo

# AWS S3
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_S3_BUCKET=joat-images
AWS_REGION=ap-northeast-2
AWS_S3_PRESIGNED_EXPIRY=300  # 5분 (초)

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# FCM (v2 때 활성화)
FCM_PROJECT_ID=
GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-service-account.json
```

## 설정 파일 위치

모든 `@Configuration` 클래스 → `joat.global.config`

| 클래스 | 역할 |
|---|---|
| `SecurityConfig` | Spring Security, JWT 필터 등록 |
| `S3Config` | S3Presigner 빈 등록 |
| `RedisConfig` | RedisTemplate 빈 등록 |
| `KafkaConfig` | Kafka 토픽 생성 |
| `JpaAuditingConfig` | @EnableJpaAuditing |

## 로컬 개발 환경

```bash
docker-compose up -d    # PostgreSQL + Redis + Kafka 실행
./gradlew bootRun       # 서버 실행
```

## 인프라 구성도 (MVP)

```
[Mobile App]
    ↓ HTTPS
[EC2/ECS] Spring Boot
    ↓              ↓
[RDS PostgreSQL]  [ElastiCache Redis]
    ↓
[S3] 이미지 저장 (CloudFront 배포)
[Kafka] 태그 처리 이벤트
```
