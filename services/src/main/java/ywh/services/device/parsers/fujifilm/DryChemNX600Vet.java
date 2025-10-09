package ywh.services.device.parsers.fujifilm;


import org.jetbrains.annotations.NotNull;
import ywh.commons.DateTime;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.impl.TcpHostCommunicatorImpl;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserInfo;
import ywh.services.device.parsers.ParsingContext;
import ywh.services.device.protocol.hl7.Hl7PartsProcessor;
import ywh.services.device.protocol.hl7.Hl7ProtocolImpl;
import ywh.services.device.protocol.hl7.Hl7ResponseBuilder;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.logging.DeviceLogger;

import java.util.concurrent.atomic.AtomicReference;

@ParserInfo(name = "Fujifilm DriChem NX 600 VET", defaultProtocol = Hl7ProtocolImpl.class, encoding = "utf-8")

public class DryChemNX600Vet extends ParserAbstract {

    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpHostCommunicatorImpl(params.getPort(), logger);
    }

    private int hostMsgId = 1;
    private String msgId = "";
    private String msgType = "R22";
    private String msh3 = "";// Instrument name as defined in Host Settings on cobas® pure
    private String msh4 = "";//Should be a copy of MSH-6 of the acknowledged message
    private String msh5 = "";//Host name as defined in Host Settings on cobas® pure
    private String msh6 = ""; //Should be a copy of MSH-4 of the acknowledged message
    private String msh21 = "LAB-28R^ROCHE"; //Should be a copy of MSH-21 of the message being acknowledged

    @Override
    public void parse(byte[] data) {
        var processor = new Hl7PartsProcessor(logger);
        var context = new ParsingContext();
        var frames = processor.getFrames(data, getCharset());
        context.putAnimalType(AnimalType.CAT);
        processor
                .setOnMSH(parts -> {
                    msgType = parts[8].split("\\^")[1];
                    msgId = parts[9];
                    msh21 = parts[20];
                    msh3 = parts[4];
                    msh4 = parts[5];
                    msh5 = parts[2];
                    msh6 = parts[3];
                    context.setAck(buildAck());
                })
                .setOnSPM(parts -> {
                    context.putOwner(parts[2]);
                })
                .setOnSAC(parts -> {
                    context.putId(parts[3]);
                })

                .setOnOBX(parts -> {
                    String name = parts[3].split("\\^")[1];
                    if (parts[2].equals("NM")) {
                        context.putDate(DateTime.tryGetFromPatternOrCurrent("yyyyMMddHHmmss", parts[19]));
                        context.put(name, parts[5]);
                    }
                });

        processor.processFrames(frames);
        fireResponse(context.getParsingResultAndReset());
    }

    private byte @NotNull [] buildAck() {

        Hl7ResponseBuilder builder = new Hl7ResponseBuilder(21);
        builder.set(1, "MSH", "^~\\&",msh3, msh4, msh5, msh6, DateTime.getHl7DateTime()) // from 1 to 7
                .set(9, "ACK^" + msgType + "^ACK", String.valueOf(hostMsgId), "P", "2.5.1") // 9 to 12
                .set(18, "UNICODE UTF-8").set(21, msh21);
        String resp = builder.buildWithAdditionalString("MSA|AA|" + msgId);
        return resp.getBytes(getCharset());
    }

}
