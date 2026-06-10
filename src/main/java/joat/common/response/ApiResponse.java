package joat.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 응답에 사용하는 공통 래퍼 클래스.
 * 성공 시 { success: true, data: ... }, 실패 시 { success: false, error: { code, message } } 형태로 직렬화된다.
 * null 필드는 @JsonInclude(NON_NULL)에 의해 JSON 출력에서 제외된다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success; // 요청 성공 여부
    private final T data;          // 성공 시 응답 데이터 (실패 시 null)
    private final ErrorBody error; // 실패 시 에러 정보 (성공 시 null)

    /** 데이터가 있는 성공 응답 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 데이터가 없는 성공 응답 (삭제, 로그아웃 등) */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /** 실패 응답 (에러 코드 + 메시지) */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    /**
     * 실패 응답 본문에 담기는 에러 정보.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;    // ErrorCode enum 이름 (예: USER_NOT_FOUND)
        private String message; // 사용자에게 노출되는 에러 메시지
    }
}
