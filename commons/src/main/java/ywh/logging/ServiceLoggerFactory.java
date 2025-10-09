package ywh.logging;

import ywh.commons.data.ConsoleColor;

public class ServiceLoggerFactory {
    private ServiceLoggerFactory() {}

    // Основний метод, який приймає IAppLogger (батьківський інтерфейс)
    public static IServiceLogger createLogger(IAppLogger appLogger, String serviceName) {
        return new IServiceLogger() {
            @Override
            public void log(String msg) {
                appLogger.info("[" + serviceName + "] ");
                appLogger.log(msg);
            }

            @Override
            public void error(String msg, Throwable ex) {
                appLogger.info("[" + serviceName + "] ");
                appLogger.error(msg, ex);
            }

            @Override
            public void error(String msg) {
                appLogger.info("[" + serviceName + "] ");
                appLogger.error(msg);
            }
        };
    }
    public static IServiceLogger createLogger(String loggerName, ConsoleColor color) {
        IAppLogger logger = AppLoggerFactory.createLogger(loggerName,loggerName, color);
        return new IServiceLogger() {
            @Override
            public void log(String msg) {
                logger.log(msg);
            }

            @Override
            public void error(String msg, Throwable ex) {
                logger.error(msg, ex);
            }

            @Override
            public void error(String msg) {
                logger.error(msg);
            }
        };
    }
    public static IServiceLogger createLogger(AppLogger appLogger, String serviceName) {
        return createLogger((IAppLogger) appLogger,serviceName);
    }

    public static IServiceLogger createLogger(DeviceLogger deviceLogger, String serviceName) {
        return createLogger((IAppLogger) deviceLogger,serviceName);
    }


}