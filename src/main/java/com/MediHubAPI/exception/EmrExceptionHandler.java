package com.MediHubAPI.exception;

import com.MediHubAPI.dto.ErrorEnvelope;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class EmrExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code(ex.getCode())
                                .message(ex.getMessage())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorEnvelope> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code("VALIDATION_ERROR")
                                .message(ex.getMessage())
                                .build())
                        .build()
        );
    }
}
