package ywh.services.device.parsers.ise;

import ywh.commons.DateTime;
import ywh.logging.DeviceLogger;
import ywh.services.communicator.ICommunicator;
import ywh.services.communicator.TcpHostCommunicator;
import ywh.services.data.models.api.Order;
import ywh.services.data.models.api.PatientData;
import ywh.services.data_processor.APIProcessor;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.ParserMetaData;
import ywh.services.device.parsers.ParsingContext;
import ywh.services.device.protocol.astm.ASTMOrderBuilder;
import ywh.services.device.protocol.astm.ASTMPartsProcessor;
import ywh.services.device.protocol.astm.ASTMProtocol;
import ywh.services.settings.data.CommunicatorSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@ParserMetaData(name = "ISE Miura ASTM", defaultProtocol = ASTMProtocol.class, encoding = "windows-1251", sendPause = 150, defaultIdleTimeout = 15000)
public class MIURA extends ParserAbstract {
    @Override
    public void parse(byte[] data) {
        ASTMPartsProcessor processor = new ASTMPartsProcessor(logger);
        List<String> barcodes = new ArrayList<>();
        var context = new ParsingContext();
        processor
                .setScipExtEtbSize(3)
                .onHeader(parts -> {
                    // Header: просто пропускаємо
                })
                .onOrder(parts -> {
                    if (!parts[2].equals(context.getId()) && !context.getId().isEmpty() && context.isNotOrderFlag()) {
                        context.putIdFromLastBarcode();
                        fireResponse(context.getParsingResultAndReset());
                    }
                    context.setBarcode(parts[2]);
                    context.putDate(DateTime.tryGetFromASTMOrCurrentV1(parts[6]));
                })
                .onResult(parts -> {
                    String[] codeParts = parts[2].split("\\^");
                    String test = codeParts.length > 4 && !codeParts[4].trim().isEmpty()
                            ? codeParts[4] : codeParts[3];
                    context.put(test, parts[3]);
                    context.putReferences(ref -> {
                                var refParts = parts[5].split(":");
                                ref.indicatorName(test)
                                        .min(refParts[0])
                                        .max(refParts[1])
                                        .unit(parts[4]);
                            }
                    );

                })
                .onQuery(parts -> {  // 0 = обробляти але не скіпати
                    if (parts[2].contains("\\")) {
                        barcodes.addAll(Arrays.asList(parts[2].split("\\\\")));
                    } else {
                        barcodes.add(parts[2]);
                    }
                    context.markAsOrder();
                })
                .onTerminator(parts -> {
                    if (context.isNotOrderFlag()) {
                        context.putIdFromLastBarcode();
                        fireResponse(context.getParsingResultAndReset());
                    }
                });

        processor.processFrames(data, charset);
        if (context.isOrderFlag()) {
            setEnquiry(barcodes);
        }
    }

    private void setEnquiry(List<String> barcodes) {
        try {
            barcodes = barcodes.stream().distinct().toList();
            if (!(protocol instanceof ASTMProtocol astmProtocol)) {
                logger.error("Only ASTM protocol is supported");
                return;
            }
            ASTMOrderBuilder builder = new ASTMOrderBuilder();
            builder.setMaxFrameSize(2024) //2048 по мануалу
                    .byLine()
                    .skipCRafterETX()
                    .addHeader(14, header -> {
                        //  1H|\\^&|||HOST-1|||||||P||A8
                        //<STX>1H|\\^&|||HOST-1|||||||P||<ETX>F0<CR><LF>
                        header[4] = "HOST-1";
                        header[11] = "P";
                        header[12] = "LIS2-A2";
                        header[13] = DateTime.getASTMDateTime();
                    })
            ;
            boolean hasAnyOrder = false;
            int patientCount = 1;
            for (String s : barcodes) {
                String barcode = s;
                barcode = barcode.trim();
                // barcode = "0000035";
                List<String> orders;
                Order orderModel = new APIProcessor(deviceSettings).getOrderById(barcode, logger);
                if (orderModel.getIndicators().isEmpty()) {
                    logger.log("No order for id: [" + barcode + "]");
                    logger.writeSeparator();
                    continue;
                }
                patientCount++;
                hasAnyOrder = true;
                // orderModel.splitByHyphenIndicators();
                orderModel.removeNonIntegerIndicators();
                orders = orderModel.getIndicators();
                PatientData patientData = orderModel.getPatientData();

                int finalPatientCount = patientCount;
                builder
                        //Full size 35, но он игнорируется.
                        .addPatient(35, patient -> {
                            patient[1] = String.valueOf(finalPatientCount);
                            patient[3] = patientData.getBirthDayInFormat("yyyyMMdd") + "-" + orderModel.getId();
                            patient[5] = patientData.getFullNameTransliterate(99).replaceFirst(" ", "^").replaceFirst(" ", "");
                            patient[7] = patientData.getBirthDayInFormat("yyyyMMdd");
                            patient[8] = patientData.getGenderByType("M", "F", "U");
                            patient[9] = "W"; // W = white
                            patient[28] = "U";
                            patient[29] = "S";
                            patient[34] = "A";// A = adult
                        });

                for (int i = 0, orderForDBSSize = orders.size(); i < orderForDBSSize; i++) {
                    int finalI = i;
                    String finalBarcode = barcode;
                    builder.addOrder(31, order -> {
                        String orderCode = orders.get(finalI);
                        order[1] = String.valueOf(finalI + 1); //SequenceNumber
                        order[2] = finalBarcode; //Patient Barcode
                        order[3] = ""; //Optional InstrumentSpecimenID
                        order[4] = "^^^" + orderCode;
                        order[5] = "R";//orderModel.getPriority("R","S");
                        order[6] = DateTime.getASTMDateTime();
                        order[11] = "N"; // Action Code OPTIONAL
                    /* C cancel request for the battery or tests named
                    A add the requested tests or batteries to the existing specimen with the patient and specimen
                    N new requests accompanying a new specimen
                    P pending specimen
                    X specimen or test already in process
                    Q treat specimen as a Q/C test specimen*/
                        order[15] = "S"; // ONLY S? SpecimenDescriptor Type of sample (serum, urine etc.) Refer to sample type table listed on the instrument
                        order[18] = patientData.getGenderByType("M", "F", "U"); // в мануале пример только M... Type of the patient (male, female, new born etc.) Refer to patient type table listed on the instrument;
                        order[19] = patientData.getFullNameTransliterate().split(" ")[0]; // в мануале пример только M... Type of the patient (male, female, new born etc.) Refer to patient type table listed on the instrument;
                        order[25] = "Q"; //ReportTypes
                    /*O order record; user asking that analysis be performed
                    F Final result
                    Z no record of this patient (in response to query)
                    Q response to query*/
                    });
                }

            }
            if (hasAnyOrder) {
                var newOrder = builder.addTerminationRecord(3, termination -> {
                    termination[1] = "1";
                    termination[2] = "N";
                }).buildWithEOT();
                astmProtocol.addOrderToQueue(newOrder);
                logger.log("Order is prepared to ASTM. Adding to queue...");

            } else logger.log("No orders to send");
        } catch (Exception ex) {
            logger.error(" Exception while making enquiry for barcode [" + String.join(", ", barcodes) + "]", ex);
        }

    }


    @Override
    public ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger) {
        return new TcpHostCommunicator(params.getPort(), logger);
    }
}