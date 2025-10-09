package ywh.logging;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.Discriminator;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Discriminator для SiftingAppender, который возвращает loggerName вместо MDC.
 */
@Data
@NoArgsConstructor
public class LoggerNameBasedDiscriminator implements Discriminator<ILoggingEvent> {
    private String key;
    private String defaultValue;
    private boolean started = false;

    @Override public void start() {started = true; }
    @Override public void stop() {started = false; }
    @Override public boolean isStarted() { return started; }
    @Override public String getKey() { return key; }

    @Override
    public String getDiscriminatingValue(ILoggingEvent event) {
        String nm = event.getLoggerName();
        return (nm == null || nm.isEmpty()) ? defaultValue : nm;
    }
}
