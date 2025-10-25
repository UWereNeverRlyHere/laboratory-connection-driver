package ywh.services.device.protocol.custom;

import ywh.logging.DeviceLogger;
import ywh.services.device.protocol.BufferedProtocolAbstract;

public class CustomProtocol extends BufferedProtocolAbstract {

    private final ITerminationStrategy terminationStrategy;
    private final IAckStrategy ackStrategy;

    public CustomProtocol(StrategyContainer strategyContainer, DeviceLogger logger, long idleTimeoutMs) {
        super(logger, idleTimeoutMs);
        this.terminationStrategy = strategyContainer.terminationStrategy();
        this.ackStrategy = strategyContainer.ackStrategy();
    }

    @Override
    public void onByte(byte b) {
        append(b);
        if (terminationStrategy.analyze(b, buf)) {
            fireFrame();
        }
        ackStrategy.analyze(b, buf).ifPresent(ack -> transport.send(ack));
    }

    @Override
    public void reset() {
        super.reset();
        ackStrategy.reset();
        terminationStrategy.reset();
    }


    public record StrategyContainer(ITerminationStrategy terminationStrategy, IAckStrategy ackStrategy) {
        public StrategyContainer(ITerminationStrategy terminationStrategy) {
            this(terminationStrategy, new IAckStrategy() {
            });
        }
    }

}
