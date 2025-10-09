package ywh.services.data.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ObservationKey {
    ID("Id"),
    OWNER("Owner"),
    ANIMAL_TYPE("Animal Type"),
    PHONE("Phone Number"),
    DATE("Date"),
    PRINT_DATE("Print Date"),
    ANIMAL_NAME("Animal Name"),
    ANIMAL_NORM_NAME("Animal Norm"),
    AGE("Age"),
    ANALYZER("Analyzer")
    ;
    private final String name;

    ObservationKey(String name) {
        this.name = name;
    }

    public static boolean hasName(String name) {
        return Arrays.stream(ObservationKey.values()).anyMatch(key -> key.getName().equals(name));
    }

    @Override
    public String toString() {
        return name;
    }
}
