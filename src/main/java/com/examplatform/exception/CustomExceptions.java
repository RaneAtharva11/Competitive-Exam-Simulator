package com.examplatform.exception;



public class CustomExceptions {

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) { super(msg); }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String msg) { super(msg); }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String msg) { super(msg); }
    }

    public static class AttemptAlreadyExistsException extends RuntimeException {
        public AttemptAlreadyExistsException(String msg) { super(msg); }
    }

    public static class AttemptAlreadySubmittedException extends RuntimeException {
        public AttemptAlreadySubmittedException(String msg) { super(msg); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) { super(msg); }
    }
}
