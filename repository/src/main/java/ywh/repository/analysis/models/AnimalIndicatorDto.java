package ywh.repository.analysis.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.repository.animals.enteties.AnimalType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimalIndicatorDto {
    private AnimalType animalType;
    private String displayName;
    private List<IndicatorDto> indicators;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorDto {
        private String code;
        private String name;
        private String printName;
        private ReferenceRangeDto referenceRange;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceRangeDto {
        private double min;
        private double max;
        private double mean;
        private String unit;
        private String text;
        private boolean isNonNumeric;
    }
}
