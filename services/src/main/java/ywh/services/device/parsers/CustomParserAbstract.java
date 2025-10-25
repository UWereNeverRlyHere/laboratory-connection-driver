package ywh.services.device.parsers;

import ywh.logging.DeviceLogger;
import ywh.services.device.protocol.IProtocol;

public abstract class CustomParserAbstract extends ParserAbstract {

    public CustomParserAbstract() {
        super();

    }

    protected abstract Class<?> terminationBuilder();

    @Override
    IProtocol createProtocol(Class<? extends IProtocol> protocolClass) {
        try {
            return protocolClass
                    .getDeclaredConstructor(terminationBuilder(),DeviceLogger.class, long.class)
                    .newInstance(logger, 15000L);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot createApi protocol: " + protocolClass.getName(), e);
        }
    }

}
