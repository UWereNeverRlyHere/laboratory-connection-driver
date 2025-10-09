package ywh.repository.analysis.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.repository.animals.enteties.AnimalType;

import java.util.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Indicator {
    private String name;
    private String code;
    private String printName;
    private final Set<String> codeVariations = new HashSet<>();
    private final Set<String> orderCodes = new HashSet<>();

    private final Map<AnimalType, ReferenceRange> referenceRanges = new HashMap<>();


    private boolean hasOrderCode(String code){
        return orderCodes.contains(code);
    }

    private boolean hasVariation(String code){
        return codeVariations.contains(code);
    }

    public Optional<ReferenceRange> getReferenceRange(AnimalType animalType) {
        return Optional.ofNullable(referenceRanges.get(animalType));
    }


    public void addReferenceRange(AnimalType animalType, ReferenceRange range) {
        referenceRanges.put(animalType, range);
    }

    public boolean hasReferenceRange(AnimalType animalType) {
        return referenceRanges.containsKey(animalType);
    }

}
