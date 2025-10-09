package ywh.services.data.models.observation;

import lombok.Getter;


public enum DeviationType {
    LOWER("нижче норми"),
    UPPER("вишче норми"),
    NORMAL("в нормі"),
    NOT_NORMAL("не в нормі"),
    UNDEFINED("—");

    @Getter
    private final String defaultText;

    DeviationType(String defaultText) {
        this.defaultText = defaultText;
    }

}