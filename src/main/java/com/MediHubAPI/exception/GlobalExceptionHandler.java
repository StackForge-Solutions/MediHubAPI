package com.MediHubAPI.exception;

import com.MediHubAPI.dto.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HospitalAPIException.class)
    public ResponseEntity<ErrorResponse> handleHospitalAPIException(HospitalAPIException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false),
                Instant.now()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getDescription(false),
                Instant.now()
        );

        errorResponse.setValidationErrors(errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            DisabledException.class,
            LockedException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthenticationExceptions(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "Authentication failed";

        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
        } else if (ex instanceof DisabledException) {
            message = "User account is disabled";
        } else if (ex instanceof LockedException) {
            message = "User account is locked";
        }

        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false),
                Instant.now()
        );
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false); // e.g. "uri=/api/test-roles/super-admin"
        String message;

        if (path.contains("/api/test-roles/super-admin")) {
            message = " Access denied: Only SUPER_ADMIN can access this endpoint.";
        } else if (path.contains("/api/test-roles/admin-or-hr")) {
            message = " Access denied: Only ADMIN or HR_MANAGER roles can access this endpoint.";
        } else if (path.contains("/api/test-roles/billing")) {
            message = " Access denied: Only BILLING_CLERK can access this endpoint.";
        } else if (path.contains("/api/test-roles/pharmacist-or-doctor")) {
            message = " Access denied: Only PHARMACIST or DOCTOR roles can access this endpoint.";
        } else if (path.contains("/api/test-roles/debug")) {
            message = " Access denied: You are not authorized to view role information.";
        } else if (path.contains("/api/users")) {
            // 🔁 Your original custom message
            message = " Cannot create any users: " + ex.getMessage();
        } else {
            message = "Access denied: You do not have the required permission.";
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                path,
                Instant.now()
        );

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

        //  NEW: Handle invalid enum (like wrong role value)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormat(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String message = "Invalid input format";

        if (cause instanceof InvalidFormatException formatEx && formatEx.getTargetType().isEnum()) {
            String invalidValue = formatEx.getValue().toString();
            Class<?> enumClass = formatEx.getTargetType();

            if (enumClass.getSimpleName().equals("ERole")) {
                boolean isCaseIssue = java.util.Arrays.stream(com.MediHubAPI.model.ERole.values())
                        .anyMatch(role -> role.name().equalsIgnoreCase(invalidValue));

                if (isCaseIssue) {
                    message = "Please enter the role in uppercase.";
                } else {
                    message = "Role not found.";
                }
            } else {
                message = "Invalid enum value.";
            }
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getDescription(false),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }




    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        // 👇 Print error to logs for debugging
        ex.printStackTrace();  //  ADD THIS LINE TO LOG DETAILS

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage(), //  USE ACTUAL ERROR MESSAGE INSTEAD OF GENERIC
                request.getDescription(false),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
//                "Slot conflict: Either the doctor or patient is already booked in this slot.", //  User-friendly message
                request.getDescription(false),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }



}
