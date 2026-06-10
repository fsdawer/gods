package joat.common.exception;

import lombok.Getter;

/**
 * 도메인 비즈니스 규칙 위반 시 발생하는 커스텀 예외.
 * ErrorCode enum을 통해 HTTP 상태코드와 에러 메시지를 함께 전달한다.
 * GlobalExceptionHandler가 이 예외를 잡아 ApiResponse.fail()로 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode; // 에러 코드 (HTTP 상태코드 + 메시지 포함)

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
