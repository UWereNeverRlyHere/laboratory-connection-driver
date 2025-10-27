package ywh.services.device.parsers.mindray;

import ywh.commons.DateTime;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.TcpClientCommunicator;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.device.parsers.IParserWithFixedPort;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.protocol.hl7.HL7V231Helper;
import ywh.services.device.protocol.hl7.Hl7PartsProcessor;
import ywh.services.device.protocol.hl7.Hl7Protocol;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.logging.DeviceLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@ParserMetaData(name ="Mindray BC 30 VET HL7", defaultProtocol = Hl7Protocol.class, encoding = "utf-8")
public class MindrayBC30Vet extends ParserAbstract implements IParserWithFixedPort {


    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpClientCommunicator(params.getHost(), getDefaultPort(), logger);
    }

    @Override
    public int getDefaultPort() {
        return 5100;
    }

    protected static final Set<String> SKIP_LIST = new HashSet<>(Arrays.asList(
            "Take Mode", "Blood Mode", "Test Mode", "Ref Group", "Remark",
            "Recheck flag", "Tube No", "Shelf No", "Charge type", "Patient type",
            "Analyzer", "AuditResult", "Project Type", "SN"
    ));

    @Override
    public void parse(byte[] data) {
        var hl7v231 = new HL7V231Helper();
        var observationData = new AtomicReference<>(new ObservationData());
        Hl7PartsProcessor processor = new Hl7PartsProcessor(logger);

        var pid = new AtomicReference<>("");
        processor
                .setOnMSH(parts -> {
                    hl7v231.setHl7Id(parts[9]);
                    hl7v231.setAckType(parts[15]);
                    hl7v231.setCoding(parts[17]);

                })
                .setOnPID(parts -> {
                    var names = parts[5].split("\\^");
                    observationData.get().put(ObservationKey.ANIMAL_NAME, names[0]);
                    observationData.get().put(ObservationKey.OWNER, names[1]);
                })
                .setOnPV1(parts -> observationData.get().putAnimalType(parts[2]))

                .setOnOBR(parts -> {
                    if (parts[3].isEmpty()) {
                        observationData.get().putId(pid.get());
                    } else
                        observationData.get().putId(parts[3]);
                    observationData.get().putDate(DateTime.tryGetFromASTMOrCurrentV1(parts[7]));
                })
                .setOnOBX(parts -> {
                    if (parts.length <5)return;
                    var name = parts[3].split("\\^")[1];
                    if (SKIP_LIST.contains(name))return;
                    var value = parts[5];
                    switch (parts[2]) {
                        case String s when s.equals("NM") && !name.contains("Histogram") && !name.contains("Scattergram") && !name.contains("line") -> {
                            if (name.equalsIgnoreCase("age")) {
                                switch (parts[6]){
                                    case "yr" -> value += " роки";
                                    case "mo" -> value += " місяці";
                                    default -> value = value + " " + parts[6];
                                }
                                observationData.get().put(ObservationKey.AGE, value);
                            } else
                                observationData.get().put(name, value);
                        }
                        case "ED" -> observationData.get().putImage(name, value.split("\\^")[4]);
                        default -> {
                        }
                    }
                });

        processor.processFrames(data, getCharset());
        var ack = hl7v231.getResponse().getBytes(getCharset());
        fireResponse(new ParsingResult(observationData.get(), ack));
    }



}
