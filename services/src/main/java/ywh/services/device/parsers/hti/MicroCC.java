package ywh.services.device.parsers.hti;


import ywh.logging.DeviceLogger;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.SerialCommunicator;
import ywh.services.data.serial_port.BaudRate;
import ywh.services.data.serial_port.SerialParams;
import ywh.services.device.ISerialParser;
import ywh.services.device.parsers.CustomParserAbstract;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.protocol.custom.CustomProtocol;
import ywh.services.settings.data.CommunicatorSettings;

@ParserMetaData(name = "Mindray BC-700 Series", defaultProtocol = CustomProtocol.class, encoding = "utf-8")
public class MicroCC extends CustomParserAbstract implements ISerialParser {
    @Override
    public SerialParams getDefaultParams() {
        return new SerialParams(BaudRate.BR_115200);
    }

    @Override
    public void parse(byte[] data) {

    }

    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return null;
    }

    @Override
    protected Class<?> terminationBuilder() {
        return null;
    }
}
