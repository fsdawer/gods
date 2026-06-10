# 프론트엔드 개발 참조 문서

## 화면 구조

```
AppNavigator
├── AuthStack
│   ├── SplashScreen
│   └── LoginScreen           ← 카카오/구글 로그인
└── MainTab (Bottom 5탭)
    ├── HomeStack
    │   ├── FeedScreen        ← 팔로잉 피드
    │   ├── PostDetailScreen  ← 포스트 상세 + 댓글
    │   └── UserProfileScreen ← 타인 프로필
    ├── ExploreStack
    │   ├── ExploreScreen     ← 전체 피드 + 트렌딩
    │   └── TagFeedScreen     ← 해시태그별 피드
    ├── CreateModal           ← 포스트 작성 (모달)
    ├── TodoStack
    │   └── TodoScreen        ← 투두리스트
    └── ProfileStack
        ├── MyProfileScreen
        └── EditProfileScreen
```

## API 연동 공통 규칙

- 모든 요청: `Authorization: Bearer {accessToken}` 헤더
- 401 응답 → refresh token으로 재발급 후 재시도 (Axios interceptor)
- 이미지 업로드: `POST /api/images/presigned-url` → S3 직접 PUT

## 응답 구조

```typescript
// 성공
{ success: true, data: T }

// 실패
{ success: false, error: { code: string, message: string } }

// 피드 페이지네이션
{ data: T[], nextCursor: string | null, hasNext: boolean }
```

## 주요 화면별 API

| 화면 | 사용 API |
|---|---|
| FeedScreen | GET /api/posts?limit=20&cursor={} |
| PostDetailScreen | GET /api/posts/{id}, GET /api/posts/{id}/comments |
| ExploreScreen | GET /api/posts/explore, GET /api/tags/trending |
| TodoScreen | GET /api/todos?date=YYYY-MM-DD |
| MyProfileScreen | GET /api/users/me |

## 백엔드 API 동시 개발 원칙

백엔드 API가 완성되면 해당 화면과 연동 코드를 함께 구현한다.
API가 새로 추가되면 화면도 즉시 업데이트.
