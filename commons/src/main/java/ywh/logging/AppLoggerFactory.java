package ywh.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import ywh.commons.data.ConsoleColor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class AppLoggerFactory {

    private AppLoggerFactory() {
    }

    // Кеш для створених AppLogger'ів
    private static final ConcurrentHashMap<String, AppLogger> LOGGER_CACHE = new ConcurrentHashMap<>();

    // ═══════════════════ ПУБЛІЧНІ МЕТОДИ ═══════════════════
    @SuppressWarnings("unused")
    public static AppLogger createLogger(Class<?> clazz, ConsoleColor color) {
        String className = clazz.getName();
        return LOGGER_CACHE.computeIfAbsent(className, key -> {
            String simpleClassName = clazz.getSimpleName();
            ch.qos.logback.classic.Logger slf4jLogger = innerCreateLogger(className, simpleClassName + ".log", color);
            return new AppLogger(slf4jLogger);
        });
    }

    public static AppLogger createLogger(Class<?> clazz) {
        String className = clazz.getName();
        return LOGGER_CACHE.computeIfAbsent(className, key -> {
            String simpleClassName = clazz.getSimpleName();
            ch.qos.logback.classic.Logger slf4jLogger = innerCreateLogger(className, simpleClassName + ".log");
            return new AppLogger(slf4jLogger);
        });
    }

    @SuppressWarnings("unused")
    public static AppLogger createLogger(String loggerName, ConsoleColor color) {
        return LOGGER_CACHE.computeIfAbsent(loggerName, key -> {
            ch.qos.logback.classic.Logger slf4jLogger = innerCreateLogger(loggerName, loggerName + ".log", color);
            return new AppLogger(slf4jLogger);
        });
    }

    public static AppLogger createLogger(String loggerName, String fileName, ConsoleColor color) {
        return LOGGER_CACHE.computeIfAbsent(loggerName, key -> {
            String finalFileName = fileName.endsWith(".log") ? fileName : fileName + ".log";
            ch.qos.logback.classic.Logger slf4jLogger = innerCreateLogger(loggerName, finalFileName, color);
            return new AppLogger(slf4jLogger);
        });
    }

    @SuppressWarnings("unused")
    public static AppLogger createLogger(String loggerName) {
        return LOGGER_CACHE.computeIfAbsent(loggerName, key -> {
            ch.qos.logback.classic.Logger slf4jLogger = innerCreateLogger(loggerName, loggerName + ".log");
            return new AppLogger(slf4jLogger);
        });
    }

    // ═══════════════════ ПРИВАТНІ МЕТОДИ ═══════════════════
    private static ch.qos.logback.classic.Logger innerCreateLogger(String loggerName, String fileName, ConsoleColor color) {
        var logger = innerCreateLogger(loggerName, fileName);
        addConsoleAppender(fileName, logger, color);
        return logger;
    }

    private static ch.qos.logback.classic.Logger innerCreateLogger(String loggerName, String fileName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);

        // Перевіряємо, чи не налаштований вже цей логер
        if (!logger.iteratorForAppenders().hasNext()) {
            try {
                // Створюємо директорію logs
                File logsDir = new File("logs");
                if (!logsDir.exists()) {
                    logsDir.mkdirs();
                }

                // ═══════════════════ FILE APPENDER ═══════════════════
                RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
                fileAppender.setContext(context);
                fileAppender.setName("FILE-" + fileName);
                fileAppender.setFile("logs/" + fileName);

                PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
                fileEncoder.setContext(context);
                fileEncoder.setCharset(StandardCharsets.UTF_8);
                fileEncoder.setPattern("[%thread] %d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
                fileEncoder.start();

                FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
                rollingPolicy.setContext(context);
                rollingPolicy.setFileNamePattern("logs/" + fileName + "%i");
                // rollingPolicy.setFileNamePattern("logs/" + fileName + ".%i.gz");
                rollingPolicy.setMinIndex(1);
                rollingPolicy.setMaxIndex(3);
                rollingPolicy.setParent(fileAppender);
                rollingPolicy.start();

                SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
                triggeringPolicy.setContext(context);
                triggeringPolicy.setMaxFileSize(FileSize.valueOf("20MB"));
                triggeringPolicy.start();

                fileAppender.setRollingPolicy(rollingPolicy);
                fileAppender.setTriggeringPolicy(triggeringPolicy);
                fileAppender.setEncoder(fileEncoder);
                fileAppender.start();

                logger.addAppender(fileAppender);
                logger.setLevel(Level.INFO);
                logger.setAdditive(false);

                System.out.println("Logger configured successfully for " + fileName);

            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                System.err.println("CRITICAL: Failed to configure Logger for " + fileName);
                System.err.println("Error: " + e.getMessage());
                System.err.println("Application will continue with default logging...");

            }
        }

        return logger;
    }

    private static void addConsoleAppender(String fileName, ch.qos.logback.classic.Logger logger, ConsoleColor color) {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(logger.getLoggerContext());
        consoleAppender.setName("CONSOLE-" + fileName);

        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(logger.getLoggerContext());
        consoleEncoder.setPattern(color.getLogJ4Color() + "([%thread] %d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n)");
        consoleAppender.setEncoder(consoleEncoder);

        consoleEncoder.start();
        consoleAppender.start();
        logger.addAppender(consoleAppender);
    }


    // ═══════════════════ УТИЛІТНІ МЕТОДИ ═══════════════════

    /**
     * Очищує кеш логерів (для тестування)
     */
    @SuppressWarnings("unused")
    public static void clearCache() {
        LOGGER_CACHE.clear();
    }

    /**
     * Повертає кількість закешованих логерів
     */
    @SuppressWarnings("unused")
    public static int getCacheSize() {
        return LOGGER_CACHE.size();
    }
}