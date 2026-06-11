# diagram-explainer 스킬

기술 개념을 ASCII 다이어그램으로 시각화해서 설명하는 Claude Code 스킬.

"왜 이게 사라져?", "이게 어떻게 동작해?", "A랑 B 차이가 뭐야?" 같은 질문을 하면 박스·화살표·레이블로 구조를 그려서 보여준다.

---

## 설치 위치

```
~/.claude/plugins/cache/karpathy-skills/andrej-karpathy-skills/1.0.0/
└── skills/
    └── diagram-explainer/
        ├── SKILL.md
        └── references/
            └── diagram-patterns.md
```

---

## 사용 방법

### 자동 트리거 (권장)

그냥 기술 질문을 하면 Claude가 알아서 스킬을 호출한다.

```
"Redis 볼륨이 뭐야?"
"JWT가 어떻게 동작해?"
"N+1 문제가 왜 생기는 거야?"
"Kafka랑 일반 API 호출 차이가 뭐야?"
```

### 명시적 호출

```
/andrej-karpathy-skills:diagram-explainer
```

슬래시 커맨드로 먼저 스킬을 로드한 뒤 질문한다.

---

## 출력 예시

### 예시 1 — "Docker 볼륨이 뭐야?"

**질문:** Docker 볼륨이 뭐야? 왜 없으면 데이터가 사라지는 거야?

**출력:**

```
지금 상황 (볼륨 없음):

  [Redis 컨테이너]
  ┌─────────────────────┐
  │ tags:trending       │  ← 메모리에만 존재
  │   #갓생  score: 42  │
  │   #공부  score: 38  │
  └─────────────────────┘

  docker-compose down 하는 순간 → 박스 통째로 날아감
  docker-compose up 하면 → 새 박스, 데이터 없음


볼륨 추가 후:

  [Redis 컨테이너]          [redis_data 볼륨]
  ┌─────────────────────┐   ┌──────────────────┐
  │ tags:trending       │──▶│ dump.rdb (파일)  │
  │   #갓생  score: 42  │   │ appendonly.aof   │
  └─────────────────────┘   └──────────────────┘

  docker-compose down → 컨테이너는 삭제되지만 볼륨 파일은 디스크에 남음
  docker-compose up → 새 컨테이너가 볼륨 파일 읽어서 데이터 복구
```

한 줄 요약: 볼륨은 컨테이너 바깥에 만들어두는 외부 저장소. 박스가 사라져도 내용물은 남는다.

---

### 예시 2 — "Kafka가 뭐야?"

**질문:** Kafka가 뭐야? 그냥 API 호출이랑 뭐가 달라?

**출력:**

```
일반 API 호출 (동기):

  [PostController]  ──태그 처리 요청──▶  [TagService]
        │                                      │
        │◀─────────────────────────────────────┘
        │         처리 완료 응답
        │
        ▼
     응답 반환 (TagService 끝날 때까지 대기)

  문제: TagService가 느리면 응답 전체가 느려짐


Kafka (비동기):

  [PostController]  ──게시물 저장──▶  [DB]
        │
        ▼
  [Kafka: post.created 이벤트 발행]
        │
        ▼
  즉시 응답 반환 (태그 처리 기다리지 않음)

        ↓ (백그라운드에서)

  [TagConsumer]  ──이벤트 수신──▶  tags 처리 + Redis score+1
```

한 줄 요약: Kafka는 "나중에 처리해" 메모를 남기는 것. 응답은 빠르고, 무거운 작업은 뒤에서 따로 처리한다.

---

### 예시 3 — "커서 페이지네이션이 뭐야?"

**질문:** 커서 페이지네이션이 오프셋이랑 뭐가 달라?

**출력:**

```
오프셋 방식:

  [DB]  SELECT * FROM posts ORDER BY created_at DESC LIMIT 20 OFFSET 40

  문제:
  ┌─────────────────────────────────────────┐
  │ 1~40번 데이터도 읽고 버림               │  ← 낭비
  │ 그 다음 20개만 반환                     │
  └─────────────────────────────────────────┘
  → 페이지가 뒤로 갈수록 느려짐


커서 방식:

  [DB]  SELECT * FROM posts WHERE id < 'cursor_uuid' LIMIT 20

  ┌─────────────────────────────────────────┐
  │ cursor 이후 데이터만 바로 읽음          │  ← 빠름
  │ 앞 데이터는 아예 건드리지 않음          │
  └─────────────────────────────────────────┘
  → 페이지가 아무리 뒤여도 속도 동일
```

한 줄 요약: 오프셋은 앞을 다 읽고 버리지만, 커서는 그 위치부터 바로 읽는다.

---

## 다이어그램 패턴

`references/diagram-patterns.md` 에 7가지 패턴 수록:

| 패턴 | 언제 사용 |
|------|-----------|
| 컨테이너/볼륨 | Docker, 인프라 구조 설명 |
| 캐시 Hit/Miss | Redis, 캐싱 전략 |
| API 요청 흐름 | REST, Kafka, 비동기 처리 |
| DB 관계 | 1:N, N:M 테이블 구조 |
| 상태 변화 | enum 상태 흐름, JWT 만료 |
| 전/후 비교 | 리팩토링, 성능 개선 |
| 이벤트/비동기 | Kafka, DLQ, 배치 |
