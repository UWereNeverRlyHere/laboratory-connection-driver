package ywh.services.device.parsers;

import ywh.services.device.protocol.IProtocol;
import ywh.services.device.protocol.custom.CustomProtocol;

public abstract class CustomParserAbstract extends ParserAbstract {

    protected abstract CustomProtocol.StrategyContainer getStrategy();

    @Override
    IProtocol createProtocol(Class<? extends IProtocol> ignored) {
        return new CustomProtocol(getStrategy(), logger, 15_000L);
    }

}
