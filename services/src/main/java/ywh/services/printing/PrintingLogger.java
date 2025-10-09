package ywh.services.printing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Клас для логування подій друку
 * В майбутньому буде відправляти дані на сервер
 */
public class PrintingLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logAttempt(String fileName, String printerName, PrintingMethod method) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        // TODO: замість System.out відправляти на сервер
        System.out.printf("[%s] ATTEMPT: %s -> %s via %s%n",
                timestamp, fileName, printerName, method.getDisplayName());
    }

    public static void logSuccess(String fileName, String printerName, PrintingMethod method, long timeMs) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        // TODO: замість System.out відправляти на сервер
        System.out.printf("[%s] SUCCESS: %s -> %s via %s (%d ms)%n",
                timestamp, fileName, printerName, method.getDisplayName(), timeMs);
    }

    public static void logFailure(String fileName, String printerName, PrintingMethod method, long timeMs, String error) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        // TODO: замість System.out відправляти на сервер
        System.out.printf("[%s] FAILURE: %s -> %s via %s (%d ms) - %s%n",
                timestamp, fileName, printerName, method.getDisplayName(), timeMs, error);
    }
}
