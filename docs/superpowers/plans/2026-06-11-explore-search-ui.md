# Explore 검색 화면 개선 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 검색 화면에서 인기태그 postCount 숫자 제거, 2열 그리드 레이아웃(1~10위), 최근 검색어 섹션 추가, Redis 볼륨 추가로 랭킹 소실 방지

**Architecture:** Redis에 볼륨을 추가해 컨테이너 재시작 시 트렌딩 데이터를 보존한다. 프론트엔드(JoatApp2)의 ExploreScreen을 수정해 postCount 숫자를 숨기고, 인기태그를 2열 그리드로 표시하며, AsyncStorage 기반 최근 검색어 기능을 추가한다. 최근 검색어는 서버 저장 없이 로컬 AsyncStorage만 사용한다.

**Tech Stack:** Spring Boot(backend) / React Native + @tanstack/react-query + AsyncStorage(frontend) / Redis(docker-compose)

---

### Task 1: Redis 볼륨 추가 (랭킹 소실 방지)

**Files:**
- Modify: `joat/docker-compose.yml`

- [ ] **Step 1: docker-compose.yml에 redis 볼륨 추가**

```yaml
# joat/docker-compose.yml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: joat
      POSTGRES_USER: joat
      POSTGRES_PASSWORD: joat
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6380:6379"
    volumes:
      - redis_data:/data
    command: redis-server --save 60 1 --appendonly yes

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

volumes:
  postgres_data:
  redis_data:
```

> `--save 60 1`: 60초마다 변경 발생 시 RDB 스냅샷 저장. `--appendonly yes`: AOF 로그 활성화 → 재시작 후 데이터 복구.

- [ ] **Step 2: 컨테이너 재시작 후 데이터 보존 확인**

```bash
# 기존 컨테이너 중지 (데이터 볼륨 유지)
cd /Users/jang/Desktop/Study/joat
docker-compose down

# 재시작
docker-compose up -d

# Redis 접속 후 trending 키 존재 확인 (게시물이 있었다면 데이터 남아 있어야 함)
docker exec -it joat-redis-1 redis-cli ZRANGE tags:trending 0 9 WITHSCORES
```

- [ ] **Step 3: 워크트리에서 커밋**

```bash
cd /Users/jang/Desktop/Study/joat
git worktree add ../joat-feat-explore-ui -b feat/explore-ui
cd ../joat-feat-explore-ui
git add docker-compose.yml
git commit -m "fix(infra): Redis 볼륨 추가로 재시작 시 트렌딩 데이터 보존"
```

---

### Task 2: ExploreScreen UI 개선

**Files:**
- Modify: `JoatApp2/src/screens/explore/ExploreScreen.tsx`

변경 내용:
1. `{tag.postCount}개` 텍스트 제거
2. 인기태그를 2열 그리드(좌·우 컬럼, 세로 구분선) + 좌 1위·2위, 아래 3위·4위 순으로 10위까지
3. 최근 검색어 섹션: 검색 실행 시 AsyncStorage 저장, 목록 표시, 항목별 삭제 + 전체 삭제

- [ ] **Step 1: AsyncStorage 패키지 설치 확인**

```bash
cd /Users/jang/Desktop/Study/JoatApp2
cat package.json | grep async-storage
```

없으면:
```bash
npm install @react-native-async-storage/async-storage
```

- [ ] **Step 2: ExploreScreen.tsx 전체 교체**

```tsx
import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Image,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useQuery } from '@tanstack/react-query';
import { ExploreStackParamList } from '../../navigation/types';
import { tagsApi } from '../../api/tags';
import { usersApi } from '../../api/users';

type Props = NativeStackScreenProps<ExploreStackParamList, 'Explore'>;

const HISTORY_KEY = 'search_history';
const MAX_HISTORY = 10;

async function loadHistory(): Promise<string[]> {
  const raw = await AsyncStorage.getItem(HISTORY_KEY);
  return raw ? JSON.parse(raw) : [];
}

async function pushHistory(keyword: string): Promise<string[]> {
  const prev = await loadHistory();
  const next = [keyword, ...prev.filter(k => k !== keyword)].slice(0, MAX_HISTORY);
  await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(next));
  return next;
}

async function removeHistoryItem(keyword: string): Promise<string[]> {
  const prev = await loadHistory();
  const next = prev.filter(k => k !== keyword);
  await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(next));
  return next;
}

async function clearHistory(): Promise<void> {
  await AsyncStorage.removeItem(HISTORY_KEY);
}

export default function ExploreScreen({ navigation }: Props) {
  const [keyword, setKeyword] = useState('');
  const [history, setHistory] = useState<string[]>([]);
  const inputRef = useRef<TextInput>(null);

  useEffect(() => {
    loadHistory().then(setHistory);
  }, []);

  const { data: trendingData } = useQuery({
    queryKey: ['trending'],
    queryFn: () => tagsApi.trending(),
    staleTime: 5 * 60 * 1000,
  });

  const { data: searchTagData } = useQuery({
    queryKey: ['tag-search', keyword],
    queryFn: () => tagsApi.search(keyword),
    enabled: keyword.trim().length > 0,
  });

  const { data: searchUserData } = useQuery({
    queryKey: ['user-search', keyword],
    queryFn: () => usersApi.searchUsers(keyword),
    enabled: keyword.trim().length > 0,
  });

  const trendingTags = (trendingData?.data.data ?? []).slice(0, 10);
  const searchTags = searchTagData?.data.data ?? [];
  const searchUsers = searchUserData?.data.data ?? [];

  // 2열 그리드용 쌍 배열: [[1위, 2위], [3위, 4위], ...]
  const tagPairs = trendingTags.reduce<(typeof trendingTags)[]>((acc, tag, i) => {
    if (i % 2 === 0) acc.push([tag]);
    else acc[acc.length - 1].push(tag);
    return acc;
  }, []);

  const navigateToTag = useCallback(async (tagName: string) => {
    setKeyword('');
    inputRef.current?.blur();
    const next = await pushHistory(tagName);
    setHistory(next);
    navigation.navigate('TagFeed', { tagName });
  }, [navigation]);

  const handleSearchSubmit = useCallback(async () => {
    const trimmed = keyword.trim();
    if (!trimmed) return;
    const next = await pushHistory(trimmed);
    setHistory(next);
    navigation.navigate('TagFeed', { tagName: trimmed });
    setKeyword('');
  }, [keyword, navigation]);

  const handleRemoveHistory = useCallback(async (item: string) => {
    const next = await removeHistoryItem(item);
    setHistory(next);
  }, []);

  const handleClearHistory = useCallback(async () => {
    await clearHistory();
    setHistory([]);
  }, []);

  const handleHistoryPress = useCallback(async (item: string) => {
    const next = await pushHistory(item);
    setHistory(next);
    navigation.navigate('TagFeed', { tagName: item });
  }, [navigation]);

  return (
    <View style={styles.container}>
      {/* 검색바 */}
      <View style={styles.searchBox}>
        <TextInput
          ref={inputRef}
          style={styles.searchInput}
          placeholder="태그 또는 사용자 검색..."
          value={keyword}
          onChangeText={setKeyword}
          returnKeyType="search"
          onSubmitEditing={handleSearchSubmit}
          autoCapitalize="none"
          autoCorrect={false}
        />
        {keyword.length > 0 && (
          <TouchableOpacity onPress={() => setKeyword('')} style={styles.clearBtn}>
            <Text style={styles.clearText}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      <ScrollView keyboardShouldPersistTaps="handled" style={styles.scroll}>
        {keyword.trim().length === 0 ? (
          <>
            {/* 인기 태그 TOP 10 — 2열 그리드 */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>인기 태그 TOP 10</Text>
              <View style={styles.divider} />
              {trendingTags.length === 0 ? (
                <Text style={styles.emptyText}>아직 태그가 없습니다.</Text>
              ) : (
                tagPairs.map((pair, rowIndex) => (
                  <View key={rowIndex} style={styles.tagGridRow}>
                    {/* 왼쪽 셀 */}
                    <TouchableOpacity
                      style={styles.tagGridCell}
                      onPress={() => navigateToTag(pair[0].name)}
                    >
                      <Text style={[
                        styles.tagRank,
                        rowIndex === 0 && styles.tagRankTop,
                      ]}>
                        {rowIndex * 2 + 1}
                      </Text>
                      <Text style={styles.tagName} numberOfLines={1}>
                        #{pair[0].name}
                      </Text>
                    </TouchableOpacity>

                    {/* 세로 구분선 */}
                    <View style={styles.verticalDivider} />

                    {/* 오른쪽 셀 */}
                    {pair[1] ? (
                      <TouchableOpacity
                        style={styles.tagGridCell}
                        onPress={() => navigateToTag(pair[1].name)}
                      >
                        <Text style={[
                          styles.tagRank,
                          rowIndex === 0 && styles.tagRankTop,
                        ]}>
                          {rowIndex * 2 + 2}
                        </Text>
                        <Text style={styles.tagName} numberOfLines={1}>
                          #{pair[1].name}
                        </Text>
                      </TouchableOpacity>
                    ) : (
                      <View style={styles.tagGridCell} />
                    )}
                  </View>
                ))
              )}
            </View>

            {/* 최근 검색어 */}
            {history.length > 0 && (
              <View style={styles.section}>
                <View style={styles.recentHeader}>
                  <Text style={styles.sectionTitle}>최근 검색어</Text>
                  <TouchableOpacity onPress={handleClearHistory}>
                    <Text style={styles.clearAllText}>전체 삭제</Text>
                  </TouchableOpacity>
                </View>
                {history.map((item) => (
                  <View key={item} style={styles.historyRow}>
                    <TouchableOpacity
                      style={styles.historyKeyword}
                      onPress={() => handleHistoryPress(item)}
                    >
                      <Text style={styles.historyText}>{item}</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      onPress={() => handleRemoveHistory(item)}
                      style={styles.historyDeleteBtn}
                    >
                      <Text style={styles.clearText}>✕</Text>
                    </TouchableOpacity>
                  </View>
                ))}
              </View>
            )}
          </>
        ) : (
          <>
            {/* 사용자 검색 결과 */}
            {searchUsers.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>사용자</Text>
                {searchUsers.map((user) => (
                  <TouchableOpacity
                    key={user.id}
                    style={styles.userRow}
                    onPress={() => navigation.navigate('UserProfile', { userId: user.id })}
                  >
                    {user.profileImageUrl ? (
                      <Image source={{ uri: user.profileImageUrl }} style={styles.avatar} />
                    ) : (
                      <View style={styles.avatarPlaceholder}>
                        <Text style={styles.avatarText}>{user.nickname[0]}</Text>
                      </View>
                    )}
                    <View style={styles.userInfo}>
                      <Text style={styles.userName}>{user.nickname}</Text>
                      {user.bio ? (
                        <Text style={styles.userBio} numberOfLines={1}>{user.bio}</Text>
                      ) : null}
                    </View>
                  </TouchableOpacity>
                ))}
              </View>
            )}

            {/* 태그 검색 결과 */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>태그</Text>
              {searchTags.length === 0 ? (
                <Text style={styles.emptyText}>"{keyword}"에 대한 태그가 없습니다.</Text>
              ) : (
                searchTags.map((tag) => (
                  <TouchableOpacity
                    key={tag.name}
                    style={styles.tagRow}
                    onPress={() => navigateToTag(tag.name)}
                  >
                    <Text style={styles.tagName}>#{tag.name}</Text>
                  </TouchableOpacity>
                ))
              )}
            </View>

            {searchUsers.length === 0 && searchTags.length === 0 && (
              <View style={styles.section}>
                <Text style={styles.emptyText}>"{keyword}"에 대한 결과가 없습니다.</Text>
              </View>
            )}
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f0f0',
  },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#e0e0e0',
  },
  searchInput: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 15,
  },
  clearBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  clearText: {
    fontSize: 14,
    color: '#aaa',
  },
  scroll: {
    flex: 1,
  },
  section: {
    backgroundColor: '#fff',
    marginTop: 8,
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#999',
    textTransform: 'uppercase',
    paddingVertical: 12,
    letterSpacing: 0.5,
  },
  divider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: '#e0e0e0',
    marginBottom: 4,
  },
  // 2열 그리드
  tagGridRow: {
    flexDirection: 'row',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#f0f0f0',
  },
  tagGridCell: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 13,
    paddingHorizontal: 4,
  },
  verticalDivider: {
    width: StyleSheet.hairlineWidth,
    backgroundColor: '#e0e0e0',
    marginVertical: 8,
  },
  tagRank: {
    width: 24,
    fontSize: 14,
    fontWeight: '700',
    color: '#4a90e2',
  },
  tagRankTop: {
    color: '#e25555',  // 1위·2위는 붉은 색으로 강조
  },
  tagName: {
    flex: 1,
    fontSize: 14,
    color: '#222',
    fontWeight: '500',
  },
  // 검색 결과 태그 행 (postCount 없는 버전)
  tagRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#f0f0f0',
  },
  // 최근 검색어
  recentHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  clearAllText: {
    fontSize: 13,
    color: '#aaa',
  },
  historyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 11,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#f0f0f0',
  },
  historyKeyword: {
    flex: 1,
  },
  historyText: {
    fontSize: 15,
    color: '#333',
  },
  historyDeleteBtn: {
    paddingLeft: 12,
    paddingVertical: 4,
  },
  // 사용자 결과
  userRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#f0f0f0',
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 12,
  },
  avatarPlaceholder: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#4a90e2',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  avatarText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 15,
    fontWeight: '600',
    color: '#222',
  },
  userBio: {
    fontSize: 13,
    color: '#888',
    marginTop: 2,
  },
  emptyText: {
    fontSize: 14,
    color: '#aaa',
    paddingVertical: 20,
    textAlign: 'center',
  },
});
```

- [ ] **Step 3: 앱 실행 후 화면 확인**

```bash
cd /Users/jang/Desktop/Study/JoatApp2
npx react-native start --reset-cache
# 또는 Expo 사용 시: npx expo start --clear
```

확인 항목:
- [ ] 검색바 아래 "인기 태그 TOP 10" 섹션에 구분선(divider) 표시
- [ ] 태그가 2열 그리드로 표시 (1위·2위가 나란히, 아래 3위·4위 순)
- [ ] 1위·2위 번호가 붉은색, 나머지는 파란색
- [ ] postCount "N개" 텍스트가 없음
- [ ] 태그를 탭하면 TagFeedScreen으로 이동하고 최근 검색어에 추가됨
- [ ] 최근 검색어 항목 우측 ✕ 탭 → 해당 항목 삭제
- [ ] "전체 삭제" 탭 → 전체 기록 삭제
- [ ] 검색어 입력 후 키보드 Return → TagFeedScreen 이동 + 최근 검색어 추가

- [ ] **Step 4: JoatApp2 커밋**

```bash
cd /Users/jang/Desktop/Study/JoatApp2
git add src/screens/explore/ExploreScreen.tsx
git commit -m "feat(explore): 인기태그 2열 그리드 + 최근 검색어 섹션 추가, postCount 숫자 제거"
```

---

### Task 3: 백엔드 머지 및 정리

- [ ] **Step 1: 백엔드 테스트 통과 확인**

```bash
cd /Users/jang/Desktop/Study/joat-feat-explore-ui
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: main 머지**

```bash
cd /Users/jang/Desktop/Study/joat
git merge feat/explore-ui --no-ff -m "merge: feat/explore-ui — Redis 볼륨 추가로 트렌딩 태그 영속화"
git worktree remove ../joat-feat-explore-ui
git branch -d feat/explore-ui
```
