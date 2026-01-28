package com.MediHubAPI.exception.scheduling.session;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SchedulingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public SchedulingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static SchedulingException badRequest(String code, String message) {
        return new SchedulingException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static SchedulingException conflict(String code, String message) {
        return new SchedulingException(HttpStatus.CONFLICT, code, message);
    }

    public static SchedulingException preconditionFailed(String code, String message) {
        return new SchedulingException(HttpStatus.PRECONDITION_FAILED, code, message);
    }

    public static SchedulingException notFound(String code, String message) {
        return new SchedulingException(HttpStatus.NOT_FOUND, code, message);
    }
}
