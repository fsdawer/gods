package joat.common.exception;

import jakarta.validation.ConstraintViolationException;
import joat.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 * 모든 컨트롤러에서 발생하는 예외를 잡아 ApiResponse.fail() 형식으로 통일된 응답을 반환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 규칙 위반 예외 처리.
     * ErrorCode에 정의된 HTTP 상태코드와 메시지를 그대로 응답에 담는다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
            .body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    /**
     * @Valid 유효성 검증 실패 예외 처리 (@RequestBody 필드 검증).
     * 첫 번째 필드 오류의 필드명과 메시지를 조합하여 응답한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message));
    }

    /**
     * @Min/@Max 등 @RequestParam·@PathVariable 제약 위반 처리 (@Validated 컨트롤러에서 발생).
     * 첫 번째 위반 메시지를 추출하여 400 응답으로 반환한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .findFirst()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message));
    }

    /**
     * 그 외 예상치 못한 예외 처리.
     * 서버 로그에 스택트레이스를 남기고 INTERNAL_ERROR 응답을 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
