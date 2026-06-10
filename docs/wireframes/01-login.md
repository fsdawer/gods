# LoginScreen — 로그인 화면

## 와이어프레임

```
┌─────────────────────────────┐
│                             │
│                             │
│        [앱 로고 / 아이콘]      │
│          joat               │
│       갓생 커뮤니티            │
│                             │
│                             │
│  ┌──────────────────────┐   │
│  │ 🟡  카카오로 시작하기  │   │
│  └──────────────────────┘   │
│                             │
│  ┌──────────────────────┐   │
│  │ 🔵  구글로 시작하기   │   │
│  └──────────────────────┘   │
│                             │
│                             │
│   갓생을 함께 기록하세요      │
│                             │
└─────────────────────────────┘
```

## 동작 흐름

1. 앱 진입 시 SplashScreen에서 토큰 확인
2. 토큰 없으면 LoginScreen으로 이동
3. 카카오 버튼 탭 → 카카오 SDK 로그인 → POST /api/auth/oauth/kakao → accessToken + refreshToken 저장 → MainTab으로 이동
4. 구글 버튼 탭 → 구글 SDK 로그인 → POST /api/auth/oauth/google → 동일 처리

## API 연동

| 액션 | API | 요청 | 응답 |
|---|---|---|---|
| 카카오 로그인 | POST /api/auth/oauth/kakao | { accessToken: "kakao_token" } | { accessToken, refreshToken } |
| 구글 로그인 | POST /api/auth/oauth/google | { idToken: "google_token" } | { accessToken, refreshToken } |
