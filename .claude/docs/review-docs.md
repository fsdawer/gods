# 코드 리뷰 참조 문서

## 프로젝트 컨벤션 체크리스트

### 구조 규칙
- [ ] 엔티티가 `*/entity/` 패키지에 있는가? (`*/domain/` 금지)
- [ ] 설정 클래스가 `joat.global.config`에 있는가?
- [ ] DTO에 `record` 사용 없는가? (`@Getter @NoArgsConstructor @AllArgsConstructor` 사용)
- [ ] 서비스 레이어가 인터페이스 + 구현체로 분리됐는가?
- [ ] Controller/Service가 인터페이스 타입으로 주입받는가?

### 코딩 규칙
- [ ] 모든 API 응답이 `ApiResponse<T>` 래퍼를 사용하는가?
- [ ] ID 타입이 UUID인가? (Long 사용 금지)
- [ ] DTO 네이밍이 `XxxRequest` / `XxxResponse`인가?
- [ ] `BaseEntity` 상속 누락 없는가?
- [ ] 비즈니스 예외는 `BusinessException(ErrorCode.XXX)` 사용하는가?
- [ ] FQCN 사용 없는가? (import 사용 원칙)

### 보안
- [ ] JWT 검증 누락 엔드포인트 없는가?
- [ ] 민감 정보(토큰, 비밀번호) 로그 출력 없는가?
- [ ] SQL Injection 가능성 없는가? (JPQL 파라미터 바인딩 확인)

### 성능
- [ ] N+1 쿼리 발생 가능성 없는가?
- [ ] `like_count`, `comment_count` COUNT 쿼리로 가져오지 않는가?
- [ ] 인덱스 없는 컬럼으로 조회하지 않는가?

### AOP 원칙
- [ ] 서비스 메서드 내부에 로깅 코드 직접 작성 없는가?
- [ ] 트랜잭션은 `@Transactional` 선언적 관리인가?
- [ ] 인증/인가는 Security 필터 계층에서만 처리하는가?

## 리뷰 결과 형식

```
## 리뷰 결과

### 🚨 블로커 (반드시 수정)
-

### ⚠️ 권고 (수정 권장)
-

### 💡 제안 (선택 개선)
-

### ✅ 잘 된 점
-
```

## MVP 범위 확인

v2 기능 (팀방, FCM 알림, 갓생 스트릭, 투두 템플릿 공유) 관련 코드가 있으면 제거 요청.
