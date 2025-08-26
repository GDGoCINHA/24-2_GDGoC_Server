package inha.gdgoc.global.exception;

import static inha.gdgoc.global.exception.GlobalErrorCode.*;
import static inha.gdgoc.global.exception.GlobalErrorCode.FORBIDDEN_USER;

import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.ErrorMeta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.error("BusinessException 발생: {}", ex.getMessage());

        ErrorCode errorCode = ex.getErrorCode();
        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, meta));
    }

    /**
     * 요청에서 필요한 헤더가 누락된 경우 400 Bad Request 응답을 반환합니다.
     *
     * 상세: 누락된 헤더 이름을 기반으로 오류 메시지를 생성하고 요청 정보를 사용해 ErrorMeta를 구성한 뒤,
     * ApiResponse 형태의 에러 바디와 함께 HTTP 400 상태로 응답합니다.
     *
     * @param ex      누락된 헤더 정보를 포함하는 MissingRequestHeaderException
     * @param request 에러 발생 시점의 HttpServletRequest (응답용 ErrorMeta 생성에 사용)
     * @return         HTTP 400 상태와 ApiResponse<Void, ErrorMeta> 형태의 에러 응답을 담은 ResponseEntity
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleMissingRequestException(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        log.error("요청 헤더 {}가 누락되었습니다.", ex.getHeaderName());

        String message = MISSING_HEADER.format(ex.getHeaderName());
        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message, meta));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("MethodArgumentNotValidException 발생: {}", message);

        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message, meta));
    }

    /**
     * 요청 파라미터의 타입 불일치(MethodArgumentTypeMismatchException)를 처리합니다.
     *
     * 예외로부터 불일치한 파라미터 이름을 사용해 사용자용 메시지를 생성하고,
     * 요청 정보로부터 ErrorMeta를 만들어 HTTP 400(Bad Request) 상태의 ApiResponse 오류 응답을 반환합니다.
     *
     * @return HTTP 400 상태와 에러 메시지·메타를 포함한 ApiResponse를 담은 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        log.error("MethodArgumentTypeMismatchException 발생: {}", ex.getMessage());
        String message = BAD_REQUEST.format(ex.getName());

        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(BAD_REQUEST.getStatus())
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message, meta));
    }

    /**
     * 검증(Bean Validation) 제약 위반 예외를 처리하고 HTTP 400 응답을 반환합니다.
     *
     * <p>발생한 ConstraintViolationException에서 첫 번째 제약 위반 메시지를 추출하여
     * ApiResponse 형태의 오류 본문과 함께 상태 코드 400(BAD_REQUEST)을 반환합니다.
     * 제약 위반 메시지가 없으면 "유효하지 않은 요청입니다."를 사용하며, 요청 정보로부터 생성한 ErrorMeta를 포함합니다.
     *
     * @param ex 처리할 ConstraintViolationException
     * @param request 현재 HTTP 요청 (응답에 포함할 메타 정보 생성에 사용)
     * @return 상태 코드 400과 ApiResponse<Void, ErrorMeta> 오류 본문을 담은 ResponseEntity
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("유효하지 않은 요청입니다.");

        log.error("ConstraintViolationException 발생: {}", message);

        ErrorMeta meta = createMeta(request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message, meta));
    }

    /**
     * 인증 관련 예외(AuthenticationCredentialsNotFoundException, AuthenticationException)를 처리하여
     * HTTP 401 Unauthorized 상태와 함께 표준 ApiResponse 오류 응답(코드: UNAUTHORIZED_USER, meta 포함)을 반환한다.
     *
     * ErrorMeta는 요청 URI와 현재 타임스탬프를 기반으로 생성된다.
     *
     * @return HTTP 401 상태와 ApiResponse<Void, ErrorMeta> 오류 본문을 담은 ResponseEntity
     */
    @ExceptionHandler({ AuthenticationCredentialsNotFoundException.class, AuthenticationException.class })
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("AuthenticationException: {}", ex.getMessage());
        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(UNAUTHORIZED_USER, meta));
    }

    /**
     * 인증된 사용자가 접근 권한이 없어 요청을 수행할 수 없을 때 이를 처리한다.
     *
     * 요청 URI와 현재 시각으로 구성된 ErrorMeta를 생성하여
     * HTTP 403 Forbidden 상태와 함께 표준 에러 응답(ApiResponse)으로 반환한다.
     *
     * @param ex      발생한 AccessDeniedException
     * @param request 에러 메타 생성에 사용되는 HttpServletRequest
     * @return HTTP 403 상태와 GlobalErrorCode.FORBIDDEN_USER 및 생성된 ErrorMeta를 포함한 ApiResponse
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(FORBIDDEN_USER, meta));
    }

    /**
     * 매핑되지 않은 요청(등록된 핸들러가 없음)을 처리하고 404 Not Found 응답을 반환합니다.
     *
     * 요청된 경로에 대응하는 핸들러를 찾지 못했을 때 호출됩니다. 요청 정보로부터 생성한 ErrorMeta를 포함한
     * ApiResponse를 바디로 하여 HTTP 404 상태 코드를 반환합니다.
     *
     * @param ex 처리된 NoHandlerFoundException 인스턴스
     * @param request 요청 URI를 사용해 ErrorMeta를 생성하는 HttpServletRequest
     * @return HTTP 404 상태와 RESOURCE_NOT_FOUND 코드 및 생성된 ErrorMeta를 포함한 ApiResponse
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        log.error("NoHandlerFoundException 발생: {}", ex.getMessage());

        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(RESOURCE_NOT_FOUND, meta));
    }

    /**
     * 모든 예외의 최상위 캐치핸들러로, 처리되지 않은 예외 발생 시 500 Internal Server Error 응답을 반환합니다.
     *
     * 상세: 발생한 예외로부터 에러 메시지를 기록하고 요청 정보를 바탕으로 ErrorMeta를 생성한 뒤,
     * ErrorCode.INTERNAL_SERVER_ERROR와 함께 ApiResponse 형태의 ResponseEntity를 상태 코드 500으로 반환합니다.
     *
     * @return 500 상태와 INTERNAL_SERVER_ERROR 코드 및 요청 메타 정보가 포함된 ApiResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void, ErrorMeta>> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("서버 내부 오류 발생: {}", ex.getMessage());

        ErrorMeta meta = createMeta(request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(INTERNAL_SERVER_ERROR, meta));
    }

    /**
     * 요청 정보를 바탕으로 ErrorMeta 객체를 생성합니다.
     *
     * @param request 메타를 만들 때 사용할 요청; ErrorMeta에는 요청의 URI와 현재 시스템 타임스탬프(밀리초)가 들어갑니다.
     * @return 요청 URI와 생성 시각을 담은 ErrorMeta 인스턴스
     */
    private ErrorMeta createMeta(HttpServletRequest request) {
        return new ErrorMeta(request.getRequestURI(), System.currentTimeMillis());
    }
}
