package ywh.services.data.models;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ywh.services.data.models.observation.ObservationData;

import java.util.Arrays;
import java.util.Objects;

public record ParsingResult(ObservationData data, byte[] ack) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsingResult that = (ParsingResult) o;
        return Objects.equals(data, that.data) && Arrays.equals(ack, that.ack);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(data);
        result = 31 * result + Arrays.hashCode(ack);
        return result;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "ParsingResult[" +
                "data=" + data + ", " +
                "ack=" + Arrays.toString(ack) +
                "]";
    }
}
