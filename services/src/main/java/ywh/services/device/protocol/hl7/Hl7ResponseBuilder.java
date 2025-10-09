package ywh.services.device.protocol.hl7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ywh.services.data.enums.SpecialBytes.*;

public class Hl7ResponseBuilder {
    private String[] array;
    private final List<String[]> arrays = new ArrayList<>();

    public Hl7ResponseBuilder(int size) {
        array = new String[size];
        Arrays.fill(array, "");
        arrays.add((Arrays.copyOf(array, array.length)));
    }


    public Hl7ResponseBuilder set(int start, String... data) {
        start = start - 1;
        for (String datum : data) {
            array[start] = datum;
            start++;
        }
        return this;
    }

    public Hl7ResponseBuilder set(int place, String data) {
        array[place - 1] = data;
        return this;
    }

    public String buildWithAdditionalString(String additionalString) {
        StringBuilder sb = new StringBuilder();
        sb.append(HL7START);
        sb.append(String.join("|", array));
        sb.append(CR).append(additionalString).append(CR).append(FS).append(CR);
        return sb.toString();
    }

    public String buildAllArrays() {
        arrays.add(array);
        StringBuilder sb = new StringBuilder();
        sb.append(HL7START);
        for (String[] strings : arrays) {
            sb.append(String.join("|", strings));
            sb.append(CR);
        }
        sb.append(FS).append(CR);
        return sb.toString();
    }

    public Hl7ResponseBuilder newArray(int size) {
        array = new String[size];
        Arrays.fill(array, "");
        arrays.add((Arrays.copyOf(array, array.length)));
        return this;

    }

    public Hl7ResponseBuilder newArray(String[] newArray) {
        arrays.add((Arrays.copyOf(this.array, this.array.length)));
        this.array = newArray;
        return this;
    }
}
