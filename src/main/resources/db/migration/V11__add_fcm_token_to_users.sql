-- FCM 디바이스 토큰: 앱 시작 시 등록, 로그아웃 시 null로 초기화
-- NULL 허용 — 미등록 유저는 NotificationConsumer에서 건너뜀
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token TEXT;
