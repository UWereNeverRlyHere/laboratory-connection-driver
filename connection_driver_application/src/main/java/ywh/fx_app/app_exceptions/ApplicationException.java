package ywh.fx_app.app_exceptions;

public class ApplicationException extends Exception {
    public ApplicationException(String message) {
        super(message);
    }
    public ApplicationException(String message, Throwable cause) {
        super(message,cause);
    }
}
