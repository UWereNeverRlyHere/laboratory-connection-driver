package ywh.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Простой файловый логгер устройства.
 * Создавайте экземпляр напрямую и передавайте его в нужные классы.
 */
public final class DeviceLogger implements IAppLogger{

    private static final HexFormat HEX = HexFormat.of().withDelimiter(" ").withUpperCase();

    private final Logger logger;
    private final AtomicReference<Charset> encoding = new AtomicReference<>();

    public DeviceLogger() {
        this("default", StandardCharsets.UTF_8);
    }


    public static DeviceLogger tryToCreateOrGetDefault(String logFileName, Charset encoding) {
        try {
            return new DeviceLogger(logFileName,encoding);
        } catch (Exception e) {
            return new DeviceLogger();
        }
    }

    /**
     * @param fileName   имя файла (без директории и расширения), совпадает с именем логгера
     * @param encoding   кодировка для строкового представления байтов
     */
    public DeviceLogger(String fileName, Charset encoding) {
        this.logger = LoggerFactory.getLogger(fileName);
        this.encoding.set(encoding != null ? encoding : StandardCharsets.UTF_8);
        createLogDirIfNotExists();
    }

    private static void createLogDirIfNotExists() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
    }
    @SuppressWarnings("unused")
    public void setEncoding(Charset encoding) {
        if (encoding != null) this.encoding.set(encoding);
    }

    /* ───────────── API ───────────── */
    @Override
    public void log(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @SuppressWarnings( "unused")
    public void hex(byte[] data) {
        if (logger.isInfoEnabled()) {
            logger.info("HEX:\n{}", HEX.formatHex(data));
        }
    }

    public void message(byte[] data) {
        String hex = HEX.formatHex(data);
        String str = new String(data, encoding.get());
        logger.info("HEX:\n{}\nSTRING:\n{}", hex, str);
    }

    public void error(String msg) {logger.error(msg);}

    public void error(String msg, Throwable ex) {
        AppLogger.logError(logger, msg, ex);
    }

    @Override
    public void close() {
        if (logger instanceof ch.qos.logback.classic.Logger logbackLogger) {
            Iterator<Appender<ILoggingEvent>> appenderIter = logbackLogger.iteratorForAppenders();

            while (appenderIter.hasNext()) {
                Appender<ILoggingEvent> appender = appenderIter.next();
                appender.stop();
            }
            logbackLogger.detachAndStopAllAppenders();
        }
    }


    public void writeSeparator() {log("---------------------------");}
}