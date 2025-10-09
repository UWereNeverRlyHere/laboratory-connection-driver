package ywh.services.device.protocol.astm;

import ywh.logging.DeviceLogger;
import ywh.services.data.enums.SpecialBytes;
import ywh.services.device.protocol.BufferedProtocolAbstract;

import java.util.Arrays;


public class ASTMProtocolImpl extends BufferedProtocolAbstract {


    public ASTMProtocolImpl(DeviceLogger logger, long idleTimeoutMs) {
        super(idleTimeoutMs);
        setLogger(logger);
    }

    private final ASTMContextManager context = new ASTMContextManager(logger, this);

    public void addOrderToQueue(ASTMOrder order) {
        context.addOrderToQueue(order);
    }

    @Override
    public void setLogger(DeviceLogger logger) {
        this.logger = logger;
        context.setLogger(logger);
        context.startOrdersScheduler();
        if (!context.isSchedulerStarted()) {
            context.startOrdersScheduler();
        }
    }

    @Override
    public void onByte(byte b) {
        try {
            SpecialBytes specialByte = getSpecialByte(b);

            switch (specialByte) {
                case ENQ -> context.sendACK(() -> logger.log("ANALYZER ------> [ENQ]"));
                case ETB, ETX -> context.sendACK(() -> logger.log("ANALYZER ------> [ETX]|[ETB]"));
                case EOT -> {
                    append(b);
                    logger.log("ANALYZER ------> [EOT]");
                    logger.log("EOT byte detected, processing message...");
                    context.setNeutral();
                    context.cancelTimeout();
                    fireFrame();
                    return;
                }
                case ACK -> context.sendNextFrame(() -> logger.log("ANALYZER ------> [ACK]"));
                case NAK -> context.handleNak(() -> logger.log("ANALYZER ------> [NAK]"));
                case null, default -> {
                }
            }
            if (context.isNotSender()) {
                append(b);
                context.setReceiver();
            }

        } catch (Exception e) {
            logger.error("Unexpected error while processing byte", e);
        }
    }


    public static SpecialBytes getSpecialByte(byte test) {
        return Arrays.stream(SpecialBytes.values()).filter(specialByte -> specialByte.getValue() == test).findFirst().orElse(null);
    }

    @Override
    public void close() {
        logger.log("Closing ASTM Protocol...");
        context.close();

        super.close();
        logger.log("ASTM Protocol closed");
    }


}
