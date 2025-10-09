package ywh.logging;

import lombok.extern.slf4j.Slf4j;
import ywh.commons.data.ConsoleColor;

@Slf4j
public final class MainLogger {
    private MainLogger() { }

    private static final AppLogger APP_LOGGER = AppLoggerFactory.createLogger("application", ConsoleColor.MAGENTA);

    public static void info(String msg) {
        APP_LOGGER.info(msg);
    }

    public static void warn(String msg) {
        APP_LOGGER.warn(msg);
    }

    public static void error(String msg) {
        APP_LOGGER.error(msg);
    }

    public static void error(String msg, Throwable ex) {
        APP_LOGGER.error(msg, ex);
    }


}