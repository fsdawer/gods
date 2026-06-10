package joat.feed.entity;

/**
 * 게시물 유형 열거형.
 * Post 엔티티의 type 컬럼에 문자열로 저장된다.
 */
public enum PostType {
    free,      // 일반 게시글 (자유 공유)
    todo_cert  // 투두 인증 게시글 (todoId와 연결됨)
}
