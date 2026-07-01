package com.akgec.hostel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class HostelException {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
        public ResourceNotFoundException(String resource, String field, Object value) {
            super(resource + " not found with " + field + " = " + value);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.GONE)
    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public static class LeaveWorkflowException extends RuntimeException {
        public LeaveWorkflowException(String message) { super(message); }
    }
}
