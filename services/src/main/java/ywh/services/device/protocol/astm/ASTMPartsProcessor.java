package ywh.services.device.protocol.astm;

import lombok.Getter;
import ywh.services.data.enums.SpecialBytes;
import ywh.logging.DeviceLogger;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Getter
public class ASTMPartsProcessor {
    private int scipExtEtbSize = 4;

    /* ─── геттери ─── */
    private Consumer<String[]> onH = x -> {
    }; // по замовчуванню нічого не робить
    private Consumer<String[]> onP = x -> {
    };
    private Consumer<String[]> onO = x -> {
    };
    private Consumer<String[]> onR = x -> {
    };
    private Consumer<String[]> onM = x -> {
    };
    private Consumer<String[]> onQ = x -> {
    };
    private Consumer<String[]> onC = x -> {
    };
    private Consumer<String[]> onL = x -> {
    };

    private final DeviceLogger logger;

    public ASTMPartsProcessor(DeviceLogger logger) {
        this.logger = logger;
    }

    public ASTMPartsProcessor setScipExtEtbSize(int scipExtEtbSize) {
        this.scipExtEtbSize = scipExtEtbSize;
        return this;
    }

    ASTMPartsProcessor ASTMPartsProcessor(DeviceLogger logger) {
        return new ASTMPartsProcessor(logger);
    }

    /* ─── сеттери ─── */
    public ASTMPartsProcessor onHeader(Consumer<String[]> onH) {
        this.onH = onH;
        return this;
    }

    public ASTMPartsProcessor onPatient(Consumer<String[]> onP) {
        this.onP = onP;
        return this;
    }

    public ASTMPartsProcessor onOrder(Consumer<String[]> onO) {
        this.onO = onO;
        return this;
    }

    public ASTMPartsProcessor onResult(Consumer<String[]> onR) {
        this.onR = onR;
        return this;
    }

    public ASTMPartsProcessor onManufacturer(Consumer<String[]> onM) {
        this.onM = onM;
        return this;
    }

    public ASTMPartsProcessor onQuery(Consumer<String[]> onQ) {
        this.onQ = onQ;
        return this;
    }

    public ASTMPartsProcessor onComment(Consumer<String[]> onC) {
        this.onC = onC;
        return this;
    }

    public ASTMPartsProcessor onTerminator(Consumer<String[]> onL) {
        this.onL = onL;
        return this;
    }


    protected String convertToString(byte[] data, Charset charset) {
        ArrayList<Byte> buffer = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            SpecialBytes specialByte = ASTMProtocolImpl.getSpecialByte(data[i]);
            if (specialByte != null) {
                switch (specialByte) {
                    case EOT, ENQ, STX -> {
                        //просто пропустить
                    }
                    case ETB, ETX -> i += scipExtEtbSize;
                    default -> buffer.add(data[i]);
                }
            } else buffer.add(data[i]);
            i++;
        }
        byte[] allBytes = new byte[buffer.size()];
        IntStream.range(0, buffer.size()).forEach(j -> allBytes[j] = buffer.get(j));
        return new String(allBytes, charset).replace("\r", "\n");

    }

    public String[] getFrames(byte[] data, Charset charset) {
        var message = convertToString(data, charset);
        return message.split("\\R");
    }

    public void processFrames(byte[] data, Charset charset) {
        var frames = getFrames(data, charset);
        processFrames(frames);
    }

    public void processFrames(String[] frames) {
        Map<String, Consumer<String[]>> processorMap = new HashMap<>();
        processorMap.put("H|", getOnH());
        processorMap.put("P|", getOnP());
        processorMap.put("O|", getOnO());
        processorMap.put("R|", getOnR());
        processorMap.put("M|", getOnM());
        processorMap.put("Q|", getOnQ());
        processorMap.put("C|", getOnC());
        processorMap.put("L|", getOnL());

        for (String frame : frames) {
            try {
                if (frame.trim().isEmpty()) continue;
                if (frame.length() < 2) {
                    logger.error("Frame too short: " + frame);
                    continue;
                }
                String[] parts = frame.split("\\|");
                String prefix = frame.length() >= 5 ? frame.substring(0, 5) : frame;
                String foundFrameType = null;

                for (String frameType : processorMap.keySet()) {
                    if (prefix.contains(frameType)) {
                        foundFrameType = frameType;
                        break;
                    }
                }
                if (foundFrameType == null)
                    logger.error("Unknown frame type in: " + frame);
                 else
                    processorMap.getOrDefault(foundFrameType, x -> {}).accept(parts);
            } catch (Exception e) {
                logger.error("Error while parsing ASTM frame: " + frame, e);
            }
        }
    }
}