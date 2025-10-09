package ywh.services.device.parsers;

import com.sun.jna.platform.win32.Guid;
import lombok.Getter;
import lombok.Setter;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.data.models.observation.ReferenceRangeResultModel;
import ywh.services.device.IParser;
import ywh.services.device.IPauseTransport;
import ywh.services.device.protocol.IProtocol;
import ywh.logging.DeviceLogger;
import ywh.services.settings.data.DeviceSettings;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ParserAbstract implements IParser {
    protected DeviceLogger logger = new DeviceLogger();
    protected DeviceSettings deviceSettings = new DeviceSettings();
    @Setter
    private String serviceName;
    private final String name;
    protected final Charset charset;
    protected final IProtocol protocol;
    private final List<ResponseListener> listeners = new CopyOnWriteArrayList<>();
    @Getter
    protected Duration sendPause;


    public static IParser create(String serviceName, ParserAbstract parser) {
        parser.setServiceName(serviceName);
        return parser;
    }

    protected ParserAbstract() {
        Class<?> parser = this.getClass();
        ParserInfo annotation = parser.getAnnotation(ParserInfo.class);
        if (annotation == null) {
            throw new IllegalStateException("Parser must be annotated with @ParserInfo");
        }

        this.name = annotation.name();
        this.serviceName = Guid.GUID.newGuid() + " - " + name;
        this.charset = Charset.forName(annotation.encoding());
        this.protocol = createProtocol(annotation.defaultProtocol());
        this.sendPause = Duration.ofMillis(annotation.sendPause());
        if (this.protocol instanceof IPauseTransport pauseTransport) {
            pauseTransport.setSendPause(sendPause);
        }
        this.protocol.setIdleTimeoutMs(annotation.defaultIdleTimeout());
    }

    private IProtocol createProtocol(Class<? extends IProtocol> protocolClass) {
        try {
            return protocolClass
                    .getDeclaredConstructor(DeviceLogger.class, long.class)
                    .newInstance(logger, 15000L);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot createApi protocol: " + protocolClass.getName(), e);
        }
    }

    @Override
    public void addResponseListener(ResponseListener listener) {
        if (listener != null) listeners.add(listener);
    }

    protected void fireResponse(ParsingResult result) {
        for (ResponseListener l : listeners) {
            try {
                l.onResponse(result);
            } catch (Exception e) {
                logger.error("Error while processing response", e);
            }
        }
    }

    public void changeSendPause(Duration pause) {
        this.sendPause = pause;
        if (this.protocol instanceof IPauseTransport pauseTransport) {
            pauseTransport.setSendPause(sendPause);
        }
    }

    @Override
    public void setLogger(DeviceLogger logger) {
        this.logger = logger;
        try {
            this.protocol.setLogger(logger);
        } catch (Exception e) {
            logger.error("Error while setting logger on defaultProtocol", e);
        }
    }

    @Override
    public IProtocol getProtocol() {
        return protocol;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void removeResponseListener(ResponseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearResponseListeners() {
        listeners.clear();
    }

    @Override
    public void setDeviceSettings(DeviceSettings settings) {
        this.deviceSettings = settings;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }



}
