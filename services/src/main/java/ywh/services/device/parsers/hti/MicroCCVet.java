package ywh.services.device.parsers.hti;


import ywh.logging.DeviceLogger;
import ywh.services.communicator.ICommunicator;
import ywh.services.data.serial_port.BaudRate;
import ywh.services.data.serial_port.SerialParams;
import ywh.services.device.ISerialParser;
import ywh.services.device.parsers.CustomParserAbstract;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.protocol.custom.CustomProtocol;
import ywh.services.device.protocol.custom.StrategyFactory;
import ywh.services.settings.data.CommunicatorSettings;

@ParserMetaData(name = "HTI MicroCC-20 Plus VET", defaultProtocol = CustomProtocol.class, encoding = "utf-8")
public class MicroCCVet extends CustomParserAbstract implements ISerialParser {
    @Override
    public SerialParams getDefaultParams() {
        return new SerialParams("COM3",BaudRate.BR_115200);
    }

    @Override
    public void parse(byte[] data) {

    }

    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
      return ICommunicator.create(CommunicatorSettings.createSerialParams(getDefaultParams()), logger);
    }


    @Override
    protected CustomProtocol.StrategyContainer getStrategy() {
        return StrategyFactory.byEndString(",TRANSFER FINISH").build();
    }
}
