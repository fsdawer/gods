# joat 앱 네비게이션 흐름

## 전체 화면 전환 다이어그램

```
앱 시작
  │
  ▼
SplashScreen
  │
  ├─ 토큰 없음 ──► LoginScreen
  │                    │
  │              카카오/구글 로그인
  │                    │
  └─ 토큰 있음 ──► MainTab ◄──────────────────────────────┐
                    │                                      │
          ┌─────────┼──────────┬──────────┬────────┐      │
          ▼         ▼          ▼          ▼        ▼      │
       HomeTab  ExploreTab  [+]버튼    TodoTab  ProfileTab │
          │         │          │          │        │       │
          ▼         ▼          ▼          ▼        ▼       │
       FeedScreen ExploreScreen CreateModal TodoScreen MyProfileScreen
          │         │                                │
          ▼         ▼                          EditProfileScreen
    PostDetailScreen TagFeedScreen
          │
     UserProfileScreen
          │
          └─────────────────────────────────────────┘
```

## Bottom Tab 구성

| 탭 | 아이콘 | 화면 |
|---|---|---|
| 홈 | 집 아이콘 | FeedScreen (팔로잉 피드) |
| 탐색 | 돋보기 아이콘 | ExploreScreen (전체 피드 + 트렌딩) |
| 만들기 | + 아이콘 | CreateModal (포스트 작성) |
| 투두 | 체크리스트 아이콘 | TodoScreen |
| 프로필 | 사람 아이콘 | MyProfileScreen |

## 화면 목록

1. SplashScreen — 앱 로딩 + 토큰 확인
2. LoginScreen — 소셜 로그인
3. FeedScreen — 팔로잉 피드 (홈)
4. ExploreScreen — 전체 피드 + 트렌딩 태그
5. TagFeedScreen — 태그별 피드
6. PostDetailScreen — 포스트 상세 + 댓글
7. UserProfileScreen — 타인 프로필
8. CreateModal — 포스트 작성 모달
9. TodoScreen — 투두리스트
10. MyProfileScreen — 내 프로필
11. EditProfileScreen — 프로필 수정
