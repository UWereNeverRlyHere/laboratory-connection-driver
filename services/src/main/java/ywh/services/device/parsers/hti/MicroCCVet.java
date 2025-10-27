package ywh.services.device.parsers.hti;
import ywh.commons.NumUtils;
import ywh.logging.DeviceLogger;
import ywh.services.communicator.ICommunicator;
import ywh.services.data.serial_port.BaudRate;
import ywh.services.data.serial_port.SerialParams;
import ywh.services.device.parsers.CustomParserAbstract;
import ywh.services.device.parsers.ISerialParser;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.parsers.ParsingContext;
import ywh.services.device.protocol.custom.CustomProtocol;
import ywh.services.device.protocol.custom.StrategyFactory;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.services.tools.HistogramBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@ParserMetaData(name = "HTI MicroCC-20 Plus VET", defaultProtocol = CustomProtocol.class, encoding = "windows-1251")
public class MicroCCVet extends CustomParserAbstract implements ISerialParser {
    @Override
    public SerialParams getDefaultParams() {
        return new SerialParams("COM3", BaudRate.BR_115200);
    }

    /*
        R,51,2025-10-08,14:15:00,Собака,999,Цельная кровь, ,1,г.,,,WBC,15.3,10^9/L,LYM#,6.6,10^9/L,MID#,2.3,10^9/L,GRA#,6.4,10^9/L,LYM%,43,%,MID%,14.8,%,GRA%,42.2,%,RBC,10.68,10^12/L,HGB,158,g/L,MCHC,364,g/L,MCH,14.8,pg,MCV,40.8,fL,RDW-CV,14.7,%,RDW-SD,24.8,fL,HCT,43.5,%,PLT,412,10^9/L,MPV,7.5,fL,PDW,2.2,fL,PCT,0.309,%,P-LCR,17.8,%,21891,W,51,2025-10-08,1,50.00,103.13,114.06,400,1.0,1.5,1.5,2.0,8.5,16.5,30.0,49.0,83.0,146.0,223.0,241.5,244.5,240.5,242.0,263.5,266.5,262.0,241.0,261.5,239.5,233.0,235.0,232.0,207.5,184.0,177.5,181.5,152.0,122.5,132.0,103.0,91.0,95.0,82.5,88.5,89.0,78.0,77.5,61.5,74.5,69.0,83.5,87.5,80.0,82.0,86.5,98.0,93.5,106.5,108.5,95.5,124.5,113.0,127.0,116.0,124.0,123.5,123.0,118.5,111.5,126.0,131.5,126.0,138.5,122.5,120.0,124.5,120.5,101.0,105.5,109.0,100.0,107.0,97.5,88.5,82.0,76.0,70.5,54.5,68.0,62.5,49.0,49.5,42.5,45.0,33.5,29.5,31.5,27.0,27.5,22.0,17.5,20.5,19.5,13.0,13.0,13.5,12.0,11.0,6.5,11.5,7.5,4.5,8.5,7.5,6.0,5.0,6.5,4.0,4.0,5.0,6.0,4.5,3.5,3.0,2.0,2.0,1.5,3.5,3.0,3.5,2.0,3.0,4.5,2.5,0.5,2.0,4.5,0.5,1.5,2.5,2.5,1.5,2.0,2.5,1.0,1.0,1.5,1.0,0.0,2.5,0.5,1.0,0.5,0.5,0.5,2.5,1.5,0.5,0.5,0.5,0.5,1.0,0.5,0.5,0.5,0.0,1.5,0.0,0.5,0.5,1.0,1.5,0.0,0.5,0.5,0.0,0.5,0.0,0.5,0.0,0.0,0.0,0.0,0.5,0.5,0.0,0.0,0.5,0.0,1.5,0.0,0.5,0.5,0.0,0.0,0.0,0.5,0.0,0.5,0.0,0.0,0.0,0.5,0.0,0.0,0.0,0.5,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.5,0.5,0.5,0.0,0.0,0.0,0.5,0.0,0.0,0.0,0.5,25.5,56.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,,,,,,,,,,,56798,W,51,2025-10-08,2,10.74,250.00,292.0,101.5,45.5,40.5,49.0,46.5,45.0,44.5,39.5,33.0,44.0,42.0,42.0,43.0,44.5,47.0,51.5,52.5,56.0,66.5,77.0,91.5,143.5,204.0,294.0,444.5,557.5,773.0,923.0,1067.0,1204.5,1273.5,1348.5,1274.0,1269.0,1197.0,1141.0,1067.5,976.5,873.5,788.0,747.5,645.0,581.5,565.5,527.5,465.5,468.5,394.0,390.0,333.0,345.5,280.0,289.5,235.5,236.0,206.5,195.5,179.0,159.5,155.0,131.5,135.5,123.5,104.0,98.5,88.5,84.5,75.5,65.5,61.0,67.5,65.5,53.5,49.0,46.0,43.5,44.5,38.0,31.0,35.0,39.5,24.0,20.0,30.0,17.5,20.0,24.5,17.5,13.0,12.0,11.0,8.5,17.0,11.5,9.0,10.5,5.0,8.5,6.0,7.0,5.0,5.5,3.0,3.5,3.0,3.0,2.0,3.0,2.0,2.0,2.5,2.0,2.5,2.0,0.0,0.5,2.5,1.0,0.5,0.0,1.0,0.5,1.0,0.0,0.0,0.0,0.5,1.0,0.0,0.0,0.5,1.0,1.5,0.0,0.5,0.5,0.5,0.0,0.5,1.0,0.0,0.5,0.0,0.0,0.0,0.0,0.5,1.0,0.5,0.5,0.5,0.5,0.0,0.5,0.0,0.0,0.0,0.0,0.5,0.5,0.5,0.0,0.0,0.5,1.0,0.0,0.5,0.0,1.0,1.0,0.0,0.5,1.5,0.0,0.0,0.0,0.5,0.5,0.0,0.0,0.0,0.0,0.5,0.5,0.0,0.0,0.5,0.0,0.5,0.0,0.0,0.5,0.5,0.0,0.5,0.0,0.0,0.5,0.5,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.5,0.5,0.0,0.0,0.5,0.0,0.0,0.0,0.0,0.0,0.5,0.0,0.0,0.0,0.0,0.0,0.0,0.5,0.0,0.5,0.0,0.0,0.0,0.0,0.0,0.5,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.5,0.0,0.0,0.5,0.0,0.0,0.0,0.0,57913,W,51,2025-10-08,3,2.00,13.16,15.0,10.5,15.0,12.5,16.0,18.5,22.5,19.0,22.0,23.5,31.5,38.0,30.0,23.5,31.0,27.5,35.0,30.5,31.5,37.5,45.5,25.0,34.0,31.5,21.5,23.0,30.5,24.5,26.5,26.0,19.0,25.5,22.0,31.0,30.0,27.5,23.5,22.5,30.5,25.0,26.5,32.0,38.0,29.5,37.5,39.0,40.5,53.5,54.5,69.5,87.0,84.0,103.0,140.5,149.5,153.0,185.0,227.5,300.5,281.0,332.0,335.5,350.5,400.0,412.0,440.0,592.0,464.5,502.5,512.5,507.5,537.0,527.5,518.0,616.5,494.5,505.0,470.5,468.0,425.0,446.0,410.5,461.5,404.0,364.5,344.5,305.5,304.5,286.5,256.0,240.5,159.0,89.0,39.0,23.5,16.0,11.0,9.0,5.0,5.5,4.0,3.0,2.0,2.5,1.5,1.5,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.5,0.0,0.0,0.0,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,37880,M,,165,TRANSFER FINISH
    */
    @Override
    public void parse(byte[] data) {
        String[] messageParts = new String(data, charset).split(",");
        List<String> analyzerCodes = new LinkedList<>(Arrays.asList("WBC", "LYM#", "MID#", "GRA#", "LYM%", "MID%", "GRA%", "RBC", "HGB", "MCHC",
                "MCH", "MCV", "RDW-CV", "RDW-SD", "HCT", "PLT", "MPV", "PDW", "PCT", "P-LCR"));
        List<String> wbcPoints = new ArrayList<>();
        List<String> rbcPoints = new ArrayList<>();
        List<String> pltPoints = new ArrayList<>();
        List<String> wbcMarkers = new ArrayList<>();
        List<String> rbcMarkers = new ArrayList<>();
        List<String> pltMarkers = new ArrayList<>();

        var context = new ParsingContext();
        context.putTempId(messageParts[1]);
        context.putId(messageParts[5]);
        context.putDate(messageParts[2] + "T" + messageParts[3]);
        context.putAnimalType(messageParts[4]);
        int i = 0;
        while (i < messageParts.length - 1) {
            if (analyzerCodes.contains(messageParts[i])) {
                context.put(messageParts[i], messageParts[i + 1]);

            } else if (messageParts[i].equals("W")) {
                switch (messageParts[i + 3]) {
                    case "1" -> {
                        wbcMarkers.add(messageParts[i + 4]);
                        wbcMarkers.add(messageParts[i + 5]);
                        wbcMarkers.add(messageParts[i + 6]);
                        wbcMarkers.add(messageParts[i + 7]);
                        i += 8;
                        while (NumUtils.isDigit(messageParts[i])) {
                            wbcPoints.add(messageParts[i]);
                            i++;
                        }
                        wbcPoints.removeLast();
                    }
                    case "2" -> {
                        rbcMarkers.add(messageParts[i + 4]);
                        rbcMarkers.add(messageParts[i + 5]);
                        i += 6;
                        while (NumUtils.isDigit(messageParts[i])) {
                            rbcPoints.add(messageParts[i]);
                            i++;
                        }
                        rbcPoints.removeLast();
                    }
                    case "3" -> {
                        pltMarkers.add(messageParts[i + 4]);
                        pltMarkers.add(messageParts[i + 5]);
                        i += 6;
                        while (NumUtils.isDigit(messageParts[i])) {
                            pltPoints.add(messageParts[i]);
                            i++;
                        }
                        pltPoints.removeLast();
                    }
                    default -> {
                    }
                }
            }
            i++;
        }
        var images = HistogramBuilder.start().addWbc(400, wbcPoints)
                .withMarkers(wbcMarkers, false)
                .addRbc(300, rbcPoints)
                .withMarkers(rbcMarkers, false)
                .addPlt(30, pltPoints)
                .withMarkers(pltMarkers, false).build();

        images.forEach(context::putImage);
        fireResponse(context.getParsingResultAndReset());
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
