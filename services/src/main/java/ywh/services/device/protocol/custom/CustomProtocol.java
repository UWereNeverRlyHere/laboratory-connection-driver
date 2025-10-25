package ywh.services.device.protocol.custom;

import ywh.logging.DeviceLogger;
import ywh.services.device.protocol.BufferedProtocolAbstract;

import java.io.ByteArrayOutputStream;

public class CustomProtocol extends BufferedProtocolAbstract {

    private final ITerminationStrategy strategy;


    protected CustomProtocol(ITerminationStrategy strategy, DeviceLogger logger, long idleTimeoutMs) {
        super(logger, idleTimeoutMs);
        this.strategy = strategy;
    }

    @Override
    public void onByte(byte b) {
        append(b);
        if (strategy.analyze(b, buf)) {
            fireFrame();
        }
    }

    @Override
    public void reset() {
        super.reset();
        strategy.reset();
    }
}
