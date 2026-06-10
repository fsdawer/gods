---
name: frontend-skill
description: React Native 화면 구현, API 연동, 컴포넌트 작성 시 사용.
---

# 프론트엔드 작업 체크리스트

## 작업 시작 전

- [ ] `.claude/docs/frontend-docs.md` 읽기 (화면 목록, API 엔드포인트 확인)
- [ ] 연동할 백엔드 API의 응답 구조 확인 (`ApiResponse<T>` 래퍼)

## 컴포넌트 작성 규칙

- [ ] 함수형 컴포넌트 + hooks 사용
- [ ] 파일명: PascalCase (컴포넌트), camelCase (훅/유틸)
- [ ] 스타일: `StyleSheet.create()` 사용 (인라인 스타일 금지)
- [ ] 에러 상태 표시 구현

## API 연동 체크리스트

- [ ] 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 포함
- [ ] 401 응답 처리 (refresh token → 재시도) Axios interceptor 확인
- [ ] 이미지 업로드: Presigned URL 플로우 사용 (서버 경유 금지)
- [ ] 피드: 커서 기반 무한스크롤 (`cursor` + `limit=20`)

## API 응답 타입 처리

```typescript
// 성공
interface ApiResponse<T> { success: true; data: T }
// 실패
interface ApiErrorResponse { success: false; error: { code: string; message: string } }
// 커서
interface CursorResponse<T> { data: T[]; nextCursor: string | null; hasNext: boolean }
```

## 백엔드 동시 개발 원칙

백엔드 API가 완성되면 해당 API를 사용하는 화면과 연동 코드를 반드시 구현한다.
API 변경이 있으면 화면도 즉시 업데이트.

## MVP 외 화면 금지

팀방, 스트릭 배지, 알림 센터는 v2. 지금 구현하지 않는다.
