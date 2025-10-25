package ywh.services.device.protocol.hl7;

import ywh.services.data.enums.SpecialBytes;
import ywh.services.device.protocol.BufferedProtocolAbstract;
import ywh.logging.DeviceLogger;

public final class Hl7Protocol extends BufferedProtocolAbstract {
    public Hl7Protocol(DeviceLogger logger, long idleTimeoutMs) {
        super(logger, idleTimeoutMs);
    }

    @Override
    public void onByte(byte b) {
        if (b == SpecialBytes.HL7START.getValue()) {
            logger.log("Got HL7 start byte, flushing buffer...");
        } else if (b == SpecialBytes.HL7END.getValue()) {
            append(b);
            logger.log("HL7 end byte detected, processing message...");
            fireFrame();
            return;
        }
        append(b);

    }

    @Override
    protected void onIdleTimeout(byte[] incompleteFrame) {
        logger.log("HL7-idle: incomplete frame detected, " + incompleteFrame.length + " bytes. Will try to parse it anyway...");
        if (incompleteFrame.length < 100) return;
        fireFrame();
    }
}
