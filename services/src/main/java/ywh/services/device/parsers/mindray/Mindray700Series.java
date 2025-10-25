package ywh.services.device.parsers.mindray;

import ywh.commons.DateTime;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.TcpHostCommunicator;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.parsers.ParsingContext;
import ywh.services.device.protocol.astm.ASTMPartsProcessor;
import ywh.services.device.protocol.astm.ASTMProtocol;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.logging.DeviceLogger;

import static ywh.services.device.parsers.mindray.MindrayBC30Vet.SKIP_LIST;

@ParserMetaData(name = "Mindray BC-700 Series", defaultProtocol = ASTMProtocol.class, encoding = "utf-8")
public class Mindray700Series extends ParserAbstract {

    @Override
    public void parse(byte[] data) {
        ASTMPartsProcessor processor = new ASTMPartsProcessor(logger);
        var context = new ParsingContext();
        processor
                .onHeader(parts -> {
                    // Header: просто пропускаємо
                })
                .onOrder(parts -> {
                    context.putId(parts[2]);
                    context.putDate(DateTime.tryGetFromASTMOrCurrentV1(parts[6]));
                })
                .onResult(parts -> {
                    String indicatorName = parts[2].split("\\^")[1];
                    if (parts[2].contains("Histogram.") || parts[2].contains("Scattergram.")) {
                        context.putImage(indicatorName, parts[3]);
                    } else {
                        if (!SKIP_LIST.contains(indicatorName)) {
                            context.put(indicatorName, parts[3]);
                            putReferences(parts, context, indicatorName);
                        }
                    }
                })
                .onTerminator(parts -> {

                });

        processor.processFrames(data, charset);
        fireResponse(context.getParsingResultAndReset());

    }

    private static void putReferences(String[] parts, ParsingContext context, String indicatorName) {
        context.putReferences(ref -> {
            var refParts = parts[5].split("\\^");
            if (parts[4].contains("&")) {
                var unitParts = parts[4].split("&");
                ref.unit(unitParts[2]);
            } else {
                ref.unit(parts[4]);
            }
            ref.indicatorName(indicatorName)
                    .min(refParts[0])
                    .max(refParts[1]);
        });
    }

    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpHostCommunicator(params.getPort(), logger);
    }
}
