package ywh.services.device.protocol.astm;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ASTMOrder {
    private final List<String> frames;
    @Getter
    private int currentIndex = 0;
    @Getter @Setter
    private int tryIndex = 1;

    public void incrementTryIndex() {
        tryIndex++;
    }

    public ASTMOrder(List<String> frames) {
        this.frames = new ArrayList<>(frames);
    }
    boolean hasNextFrame() {
        return currentIndex < frames.size();
    }
    public boolean hasNoNextFrame() {
        return currentIndex >= frames.size();
    }

    public boolean isLastFrame() {
        return currentIndex == frames.size();
    }

    public Optional<String> getNextFrame() {
        if (currentIndex < frames.size()) {
            return Optional.ofNullable(frames.get(currentIndex++));
        }
        return Optional.empty();
    }

    public String getFullOrder() {
        StringBuilder sb = new StringBuilder();
        for (String frame : frames) {
            sb.append(frame);
        }
        return sb.toString();
    }

    public String getFullHexOrder() {
        HexFormat hex = HexFormat.of().withUpperCase().withDelimiter(" ");

        return frames.stream()
                .map(frame -> hex.formatHex(frame.getBytes()))
                .collect(Collectors.joining(" "));
    }

    public void reset() {
        currentIndex = 0;
    }
}
