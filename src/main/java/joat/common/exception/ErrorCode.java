package joat.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 오류 코드 정의 열거형.
 * BusinessException에 담겨 GlobalExceptionHandler로 전달되며
 * HTTP 상태코드(status)와 사용자 메시지(message)를 함께 관리한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "이미 팔로우 중입니다."),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST, "팔로우 중이 아닙니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    OAUTH_FAILED(HttpStatus.BAD_REQUEST, "소셜 로그인에 실패했습니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시물을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시물 접근 권한이 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "게시물을 수정할 권한이 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시물입니다."),
    NOT_LIKED(HttpStatus.BAD_REQUEST, "좋아요하지 않은 게시물입니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),

    // Todo
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "투두를 찾을 수 없습니다."),
    TODO_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "투두 항목을 찾을 수 없습니다."),
    TODO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "투두 접근 권한이 없습니다."),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀방을 찾을 수 없습니다."),
    TEAM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "팀방 접근 권한이 없습니다."),
    TEAM_FORBIDDEN(HttpStatus.FORBIDDEN, "팀방 관리 권한이 없습니다."),
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "이미 팀방 멤버입니다."),
    NOT_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "팀방 멤버가 아닙니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;  // API 응답에 사용할 HTTP 상태코드
    private final String message;     // 클라이언트에 노출되는 에러 메시지
}
