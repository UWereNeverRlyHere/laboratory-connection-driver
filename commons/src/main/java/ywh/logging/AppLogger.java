package ywh.logging;

import ch.qos.logback.classic.Logger;
import ywh.commons.ConsoleUtil;
import ywh.commons.DateTime;
import ywh.commons.data.ConsoleColor;

import java.util.stream.IntStream;


public class AppLogger implements IAppLogger {
    @Override
    public void log(String msg) {
        info(msg);
    }

    private final Logger logger;

    public AppLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void warn(String msg) {
        logger.warn(msg);
    }

    public void error(String msg) {
        logger.error(msg);
    }
    @Override
    public void error(String msg, Throwable ex) {
        AppLogger.logError(logger,msg, ex);
    }


    public void info(String msg, ConsoleColor color) {
        print(color, msg);
        logger.info(msg);
    }

    public void warn(String msg, ConsoleColor color) {
        print(color, msg);
        logger.warn(msg);
    }

    public void error(String msg, ConsoleColor color) {
        print(color, msg);
        logger.error(msg);
    }

    public void error(String msg, Throwable ex, ConsoleColor color) {
        print(color, msg);
        AppLogger.logError(logger,msg, ex);
    }

    public void print(ConsoleColor color, String message) {
        ConsoleUtil.print(color, "["+logger.getName() + "] " + DateTime.getDateTime() + message);
    }

    /**
     * Універсальний метод для логування помилок з Throwable
     */
    public static void logError(org.slf4j.Logger logger, String msg, Throwable ex) {
        ex.printStackTrace();
        StringBuilder exBuilder = new StringBuilder();
        IntStream.range(0, ex.getStackTrace().length)
                .forEach(i -> exBuilder.append(" Class name: [")
                        .append(ex.getStackTrace()[i].getClassName())
                        .append("] line: [")
                        .append(ex.getStackTrace()[i].getLineNumber())
                        .append("] exception: [")
                        .append(ex)
                        .append("]\n"));

        logger.error(msg);
        logger.error("Stack trace:\n{}", exBuilder);
    }
    @Override
    public void close() {
        if (logger instanceof ch.qos.logback.classic.Logger logbackLogger) {
            logbackLogger.detachAndStopAllAppenders();
        }

    }

}
