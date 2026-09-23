package com.rrjaggery.common.exception;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.common.dto.ErrorResponse;
import com.rrjaggery.common.dto.FieldErrorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldErrorDetail> details = bindingResult.getFieldErrors().stream()
                .map(err -> new FieldErrorDetail(err.getField(), err.getRejectedValue(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse("VALIDATION_FAILED", "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument encountered: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state encountered: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("BUSINESS_RULE_VIOLATION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler({SecurityException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(Exception ex) {
        log.warn("Security/AccessDenied exception encountered: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("FORBIDDEN", ex.getMessage() != null ? ex.getMessage() : "Access is denied.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }
}
