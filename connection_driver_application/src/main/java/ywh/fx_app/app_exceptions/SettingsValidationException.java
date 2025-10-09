package ywh.fx_app.app_exceptions;

public class SettingsValidationException extends Exception {
    public SettingsValidationException(String message) {
        super(message);
    }

    public SettingsValidationException(String message, Throwable cause) {
        super(message,cause);
    }
}
