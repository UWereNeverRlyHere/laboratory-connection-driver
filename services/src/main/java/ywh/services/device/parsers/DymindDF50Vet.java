package ywh.services.device.parsers;

import org.jetbrains.annotations.NotNull;
import ywh.commons.DateTime;
import ywh.logging.DeviceLogger;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.TcpHostCommunicator;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.device.protocol.hl7.Hl7PartsProcessor;
import ywh.services.device.protocol.hl7.Hl7ProtocolImpl;
import ywh.services.settings.data.CommunicatorSettings;
@ParserInfo(name ="Dymind DF50 VET HL7", defaultProtocol = Hl7ProtocolImpl.class, encoding = "utf-8")

public class DymindDF50Vet extends ParserAbstract {

    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpHostCommunicator(params.getPort(), logger);
    }


    @Override
    public void parse(byte[] data) {
        var processor = new Hl7PartsProcessor(logger);
        var context = new ParsingContext();
        var frames = processor.getFrames(data, getCharset());
        processor
                .setOnMSH(parts -> context.setAck(buildAck(parts[9], parts[15])))
                .setOnOBR(parts -> {
                    context.putId(parts[3]);
                    context.putDate(DateTime.tryGetFromPatternOrCurrent("yyyyMMddHHmmss", parts[6]));
                })
                .setOnOBX(parts -> {
                    String name = parts[3].split("\\^")[1];
                    String value = parts[5];
                    switch (parts[2]) {
                        case "NM" -> {
                            if (name.equals("MCHC")){
                                try {
                                    var parsed = Double.parseDouble(value);
                                    var result = parsed / 10.0;
                                    value = String.format("%.1f", result).replace(".", ",");
                                }catch (Exception ignored){}
                            }
                            context.put(name, value);
                        }
                        case "ED" -> context.putImage(name, value.split("\\^")[4]);
                        case "IS" -> {
                            if(name.equals("Ref Group"))
                                context.putAnimalType(AnimalType.define(value));
                        }
                        default -> {}
                    }
                });

        processor.processFrames(frames);
        addColorationIndex(context.getObservationData());
        fireResponse(context.getParsingResultAndReset());
    }

    //DOGS (HGB*5.5) : (RBC*150)
    //CATS (HGB*6.5) : (RBC*110)
    //THIR (HGB*9) : (RBC*155)
    private void addColorationIndex(ObservationData observationData){
            try {
                var hgb = observationData.getValue("HGB");
                var rbc = observationData.getValue("RBC");
                var hgbInd = observationData.getAnimalType() == AnimalType.CAT ? 6.5 : 5.5;
                var rbcInd = observationData.getAnimalType() == AnimalType.CAT ? 110 : 150;
                if (hgb.isPresent() && rbc.isPresent()){
                    var hgbValue = Double.parseDouble(hgb.get().replace(",","."));
                    var rbcValue = Double.parseDouble(rbc.get().replace(",","."));
                    var colorationIndex = (hgbValue*hgbInd) / (rbcValue*rbcInd);
                    observationData.put("CP", String.format("%.2f", colorationIndex).replace(".", ","));
                }
            }catch (Exception ex){
                logger.error("Error while calculating coloration index", ex);
            }
    }

    private byte @NotNull [] buildAck(String id, String ackType) {
        var responseBuilder = "\u000BMSH|^~\\&|||||" +
                DateTime.getHl7DateTime() +
                "||ACK^R01|" +
                id +
                "|P|2.3.1||||" +
                ackType +
                "||UNICODE|||" +
                "\r" +
                "MSA|" +
                "AA|" +
                id +
                "|Message accepted|||0|\r\u001C\r";
        return responseBuilder.getBytes(getCharset());
    }

}
