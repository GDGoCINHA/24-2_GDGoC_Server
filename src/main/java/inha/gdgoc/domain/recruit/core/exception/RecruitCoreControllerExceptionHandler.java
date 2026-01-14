package inha.gdgoc.domain.recruit.core.exception;

import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import inha.gdgoc.domain.recruit.core.controller.RecruitCoreController;

@Slf4j
@RestControllerAdvice(assignableTypes = RecruitCoreController.class)
public class RecruitCoreControllerExceptionHandler {

    @ExceptionHandler(RecruitCoreAlreadyAppliedException.class)
    public ResponseEntity<RecruitCoreApplicationErrorResponse> handleAlreadyApplied(
        RecruitCoreAlreadyAppliedException ex
    ) {
        log.debug("RecruitCoreAlreadyAppliedException: {}", ex.getMessage());
        var code = ex.getErrorCode();
        RecruitCoreApplicationErrorResponse body = RecruitCoreApplicationErrorResponse.of(
            code.getCode(),
            code.getMessage(),
            ex.getSession(),
            ex.getApplicationId()
        );
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(RecruitCoreApplicationNotFoundException.class)
    public ResponseEntity<RecruitCoreApplicationErrorResponse> handleNotFound(
        RecruitCoreApplicationNotFoundException ex
    ) {
        log.debug("RecruitCoreApplicationNotFoundException: {}", ex.getMessage());
        var code = ex.getErrorCode();
        RecruitCoreApplicationErrorResponse body = RecruitCoreApplicationErrorResponse.of(
            code.getCode(),
            code.getMessage()
        );
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
