package com.ledgerpay.common.error;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<ApiFieldError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ApiFieldError(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .sorted(Comparator.comparing(
                                ApiFieldError::field
                        ))
                        .toList();

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {
        List<ApiFieldError> fieldErrors =
                exception.getAllValidationResults()
                        .stream()
                        .flatMap(result ->
                                result.getResolvableErrors()
                                        .stream()
                                        .map(error ->
                                                new ApiFieldError(
                                                        result
                                                                .getMethodParameter()
                                                                .getParameterName(),
                                                        error.getDefaultMessage()
                                                )
                                        )
                        )
                        .toList();

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        ApiFieldError fieldError = new ApiFieldError(
                exception.getName(),
                "Value has an invalid type"
        );

        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_PARAMETER",
                        "Request parameter is invalid",
                        List.of(fieldError)
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        "MALFORMED_REQUEST",
                        "Request body is malformed or contains an invalid value",
                        List.of()
                )
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        exception.getCode(),
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiError> handleConflict(
            ResourceConflictException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        exception.getCode(),
                        exception.getMessage(),
                        List.of()
                ));
    }
}