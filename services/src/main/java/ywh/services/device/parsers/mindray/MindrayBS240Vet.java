package ywh.services.device.parsers.mindray;

import ywh.commons.DateTime;
import ywh.commons.TextUtils;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.impl.TcpHostCommunicatorImpl;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserInfo;
import ywh.services.device.protocol.astm.ASTMPartsProcessor;
import ywh.services.device.protocol.hl7.Hl7ProtocolImpl;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.logging.DeviceLogger;

import java.util.concurrent.atomic.AtomicReference;

@ParserInfo(name = "Mindray BS 240 ASTM", defaultProtocol = Hl7ProtocolImpl.class, encoding = "windows-1251")
public class MindrayBS240Vet extends ParserAbstract {


    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpHostCommunicatorImpl(params.getPort(), logger);
    }

    @Override
    public void parse(byte[] data) {
        var observationData = new AtomicReference<>(new ObservationData());
        var sampleId = new AtomicReference<>("");
        var analysisCode = new AtomicReference<>("");
        ASTMPartsProcessor processor = new ASTMPartsProcessor(logger);
        var frames = processor.getFrames(data, charset);

        processor.onOrder(parts -> {
                    sampleId.set(parts[2].split("\\^")[0]);
                    analysisCode.set(parts[3]);
                })
                .onResult(parts -> {
                    String[] codeParts = parts[2].split("\\^");
                    String[] resultParts = parts[3].split("\\^^");
                    observationData.get().put(codeParts[0], resultParts[0].substring(0, 5));
                    observationData.get().putDate(DateTime.tryGetFromASTMOrCurrentV2(parts[11]));
                })
                .onComment(parts -> {
                    // Пропускаємо коментарі
                })
                .onQuery(parts -> {
                    //  barcodeForOrder = parts[2].contains("^") ? parts[2].split("\\^")[1] : parts[3];
                    // sqNum = parts[1];
                    setEnquiry();
                    // isOrder = true;
                })
                .onTerminator(parts -> {

                    if (TextUtils.isNullOrEmpty(analysisCode.get()))
                        analysisCode.set(sampleId.get());
                    observationData.get().putId(analysisCode.get());
                    fireResponse(new ParsingResult(observationData.get(), new byte[0]));

                });

        processor.processFrames(frames);
    }

    private void setEnquiry() {
  /*      try {
            StandardOrder orderModel = apiClientService.getOrderByBarcode(barcodeForOrder.trim());
            List<String> orderCodes = orderModel.getIndicators();
            if (orderCodes.isEmpty()) {
                logWriter.write(Messages.NO_ORDERS_FOR_BARCODE.get(barcodeForOrder));
                logWriter.writeSeparator();
                isOrder = false;
                return;
            }
            System.out.println("lims orders2 " + orderCodes);

            // Будуємо ID з padding до 5 символів
            StringBuilder id = new StringBuilder(barcodeForOrder);
            while (id.length() < 5) {
                id.append("0");
            }

            ASTMOrderBuilder builder = new ASTMOrderBuilder()
                    .setMaxFrameSize(2024) // BS-240 використовує 2024!
                    .addHeader(14, header -> {
                        header[1] = "\\^&";
                        header[4] = "AnalyzerDriver^^";
                        header[11] = "SA";
                        header[12] = "1394-97";
                        header[13] = DateTimeUtil.getASTMDateTime();
                    })
                    .addPatient(35, patient -> patient[1] = sqNum)
                    .addOrder(31, order -> {
                        order[1] = sqNum;
                        order[2] = id + "^^";
                        order[3] = barcodeForOrder;
                        order[4] = ASTMOrderBuilder.toAstmOrderString(orderCodes, ind -> ind + "^^^");

                        order[5] = orderModel.getIsCito() && configs.isCITOAllowed() ? "S" : "R";
                        order[6] = DateTimeUtil.getASTMDateTime();
                        order[7] = DateTimeUtil.getASTMDateTime();
                        order[11] = "N";
                        order[25] = "Q";
                    })
                    .addTerminationRecord(3, termination -> {
                        termination[1] = sqNum;
                        termination[2] = "N";
                    });

            AstmOrderModel result = builder.buildWithEOT();
            astmOrder.add(result);
            logWriter.write(Messages.ASTM_ORDER_PREPARED.get());

        } catch (Exception ex) {
            System.out.println("no orderForDBS");
            logWriter.write(Messages.NO_ORDERS_FOR_BARCODE.get(barcodeForOrder));
            logWriter.writeSeparator();
            isOrder = false;
        }*/
    }
}

