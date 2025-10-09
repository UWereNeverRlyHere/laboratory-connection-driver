package ywh.fx_app.app_exceptions;

public class ApplicationRuntimeException extends RuntimeException {
    public ApplicationRuntimeException(String message) {
        super(message);
    }
    public ApplicationRuntimeException(String message, Throwable cause) {
        super(message,cause);
    }
}
