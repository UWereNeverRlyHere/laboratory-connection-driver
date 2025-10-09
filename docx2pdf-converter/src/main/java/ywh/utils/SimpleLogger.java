package ywh.utils;

import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class SimpleLogger {
    private static final String MAGENTA = "\u001B[35m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 50; // 50MB

    private final String logFileName;
    private final String oldLogFileName;
    private final String encoding;
    private final Path logFile;
    private final Path oldLogFile;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile BufferedWriter bufferedWriter;

    public SimpleLogger(String name, String encoding) {
        this.logFileName = "logs\\"+ name + "_LOG.log";
        this.oldLogFileName ="logs\\"+ name + "_LOG_OLD.log";
        this.encoding = encoding != null ? encoding : StandardCharsets.UTF_8.name();
        this.logFile = Path.of(logFileName);
        this.oldLogFile = Path.of(oldLogFileName);

        checkFileSize();
        createWriter();
    }

    public SimpleLogger(String name) {
        this(name, StandardCharsets.UTF_8.name());
    }

    private void checkFileSize() {
        lock.lock();
        try {
            if (Files.exists(logFile) && Files.size(logFile) > MAX_FILE_SIZE) {
                closeWriter();

                // Видаляємо старий backup файл якщо існує
                if (Files.exists(oldLogFile)) {
                    Files.deleteIfExists(oldLogFile);
                }

                // Перейменовуємо поточний файл в backup
                Files.move(logFile, oldLogFile);

                createWriter();

                System.out.println(CYAN + "[LOG] Файл логу досягнув максимального розміру. Створено backup." + RESET);
            }
        } catch (IOException ex) {
            System.err.println(RED + "[ERROR] Помилка при ротації файлу логу: " + ex.getMessage() + RESET);
            ex.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void createWriter() {
        if (bufferedWriter == null) {
            try {
                bufferedWriter = Files.newBufferedWriter(logFile,
                        java.nio.charset.Charset.forName(encoding),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException ex) {
                System.err.println(RED + "[ERROR] Неможливо створити writer для логу: " + ex.getMessage() + RESET);
                ex.printStackTrace();
            }
        }
    }

    private void closeWriter() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
                bufferedWriter = null;
            } catch (IOException ex) {
                System.err.println(RED + "[ERROR] Помилка при закритті writer: " + ex.getMessage() + RESET);
                ex.printStackTrace();
            }
        }
    }

    public void write(String entry) {
        lock.lock();
        try {
            checkFileSize();
            if (bufferedWriter != null) {
                bufferedWriter.write(entry);
                bufferedWriter.flush();
            }
        } catch (IOException ex) {
            System.err.println(RED + "[ERROR] Помилка запису в лог: " + ex.getMessage() + RESET);
            closeWriter();
            createWriter(); // Спробуємо відновити writer
        } finally {
            lock.unlock();
        }
    }

    public void info(String message) {
        String timestamp = getCurrentTimestamp();
        String logEntry = String.format("%s [INFO] %s%n", timestamp, message);

        // Запис у файл
        write(logEntry);

        // Вивід у консоль з кольором
        System.out.println(MAGENTA + timestamp + " [INFO] " + message + RESET);
    }

    public void debug(String message) {
        String timestamp = getCurrentTimestamp();
        String logEntry = String.format("%s [DEBUG] %s%n", timestamp, message);

        write(logEntry);
        System.out.println(CYAN + timestamp + " [DEBUG] " + message + RESET);
    }

    public void warn(String message) {
        String timestamp = getCurrentTimestamp();
        String logEntry = String.format("%s [WARN] %s%n", timestamp, message);

        write(logEntry);
        System.out.println(YELLOW + timestamp + " [WARN] " + message + RESET);
    }

    public void error(String message) {
        String timestamp = getCurrentTimestamp();
        String logEntry = String.format("%s [ERROR] %s%n", timestamp, message);

        write(logEntry);
        System.err.println(RED + timestamp + " [ERROR] " + message + RESET);
    }

    public void error(String message, Throwable ex) {
        if (ex == null) {
            error(message);
            return;
        }

        // Ігноруємо деякі типи винятків
        if (ex instanceof SocketException ||
                (ex.getMessage() != null && ex.getMessage().contains("400 for URL"))) {
            return;
        }

        String timestamp = getCurrentTimestamp();
        StringBuilder stackTrace = new StringBuilder();

        // Форматуємо stack trace
        IntStream.range(0, ex.getStackTrace().length)
                .forEach(i -> stackTrace.append(String.format(
                        "    at %s.%s(%s:%d)%n",
                        ex.getStackTrace()[i].getClassName(),
                        ex.getStackTrace()[i].getMethodName(),
                        ex.getStackTrace()[i].getFileName(),
                        ex.getStackTrace()[i].getLineNumber()
                )));

        String logEntry = String.format("%s [ERROR] %s%nException: %s%nStack trace:%n%s%n",
                timestamp, message, ex.toString(), stackTrace);

        write(logEntry);

        // Вивід у консоль
        System.err.println(RED + BOLD + timestamp + " [ERROR] " + message + RESET);
        System.err.println(RED + "Exception: " + ex.toString() + RESET);
        ex.printStackTrace(); // Стандартний stack trace для консолі
    }

    public void writeSeparator() {
        String separator = String.format("%n----------------------%n");
        write(separator);
        System.out.println(MAGENTA + "----------------------" + RESET);
    }

    public void writeHex(byte b) {
        String hexString = String.format("%02X ", b);
        write(hexString);
        System.out.print(CYAN + hexString + RESET);
    }

    public void writeHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;

        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02X ", b));
        }

        String hexString = hexBuilder.toString();
        write(hexString + System.lineSeparator());
        System.out.println(CYAN + "[HEX] " + hexString + RESET);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(LOG_FORMATTER);
    }

    public void close() {
        lock.lock();
        try {
            closeWriter();
        } finally {
            lock.unlock();
        }
    }

    // Getters
    public String getLogPath() {
        return logFile.toAbsolutePath().toString();
    }

    public String getOldLogPath() {
        return oldLogFile.toAbsolutePath().toString();
    }

    // Статичний метод для створення логгера з ім'ям класу
    public static SimpleLogger forClass(Class<?> clazz) {
        return new SimpleLogger(clazz.getSimpleName());
    }

    // Метод для створення логгера з кастомним ім'ям
    public static SimpleLogger named(String name) {
        return new SimpleLogger(name);
    }
}
