package ywh.services.device.protocol.hl7;

import lombok.Getter;
import ywh.commons.TextUtils;
import ywh.logging.DeviceLogger;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class Hl7PartsProcessor {
    /* ─── геттеры ─── */
    private Consumer<String[]> onMSH = x -> {
    };  // по умолчанию ничего не делает
    private Consumer<String[]> onPID = x -> {
    };
    private Consumer<String[]> onOBR = x -> {
    };
    private Consumer<String[]> onOBX = x -> {
    };
    private Consumer<String[]> onPV1 = x -> {
    };
    private Consumer<String[]> onSPM = x -> {
    };
    private Consumer<String[]> onSAC = x -> {
    };
    private final DeviceLogger logger;

    public Hl7PartsProcessor(DeviceLogger logger) {
        this.logger = logger;
    }

    public Hl7PartsProcessor Hl7PartsProcessor(DeviceLogger logger) {
        return new Hl7PartsProcessor(logger);
    }

    /* ─── сеттеры ─── */
    public Hl7PartsProcessor setOnMSH(Consumer<String[]> onMSH) {
        this.onMSH = onMSH;
        return this;
    }

    public Hl7PartsProcessor setOnPID(Consumer<String[]> onPID) {
        this.onPID = onPID;
        return this;
    }

    public Hl7PartsProcessor setOnOBR(Consumer<String[]> onOBR) {
        this.onOBR = onOBR;
        return this;
    }

    public Hl7PartsProcessor setOnOBX(Consumer<String[]> onOBX) {
        this.onOBX = onOBX;
        return this;
    }

    public Hl7PartsProcessor setOnPV1(Consumer<String[]> onPV1) {
        this.onPV1 = onPV1;
        return this;
    }

    public Hl7PartsProcessor setOnSPM(Consumer<String[]> onSPM) {
        this.onSPM = onSPM;
        return this;
    }
   public Hl7PartsProcessor setOnSAC(Consumer<String[]> onSAC) {
        this.onSAC = onSAC;
        return this;
    }

    public String[] getFrames(byte[] frame, Charset charset) {
        var message = TextUtils.toString(frame, charset);
        var split = message
                .replaceAll("[\\u000B\\u001C]", "")
                .split("\\R");
        return split;
    }
    public void processFrames(byte[] data, Charset charset) {
        var frames = getFrames(data, charset);
        processFrames(frames);
    }
    public void processFrames(String[] frames) {
        Map<String, Consumer<String[]>> processorMap = Map.of(
                "MSH", getOnMSH(),
                "PID", getOnPID(),
                "PV1", getOnPV1(),
                "OBR", getOnOBR(),
                "OBX", getOnOBX(),
                "SPM", getOnSPM(),
                "SAC", getOnSAC()
        );

        for (var frame : frames) {
            try {
                String[] parts = frame.split("\\|");
                String frameType = frame.substring(0, 3);
                processorMap.getOrDefault(frameType, x -> {
                }).accept(parts);
            } catch (Exception e) {
                logger.error("Error while parsing frame data", e);
            }
        }
    }
}