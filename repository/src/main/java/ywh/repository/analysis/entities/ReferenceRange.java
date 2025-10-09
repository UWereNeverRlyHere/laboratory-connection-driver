package ywh.repository.analysis.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.repository.animals.enteties.AgeRange;
import ywh.repository.animals.enteties.AnimalType;
import ywh.repository.animals.enteties.Gender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceRange {
    private double min;
    private double max;
    private double mean;
    @Builder.Default
    private String text = "";
    private AnimalType animalType;
    private Gender gender;
    private AgeRange ageRange;
    private boolean isNonNumeric;
    private Unit unit;

    // Додатковий конструктор для зворотної сумісності
    public ReferenceRange(AnimalType animalType, double min, double max, String text) {
        this.animalType = animalType;
        this.min = min;
        this.max = max;
        this.text = text;
    }
        public ReferenceRange(AnimalType animalType, String text) {
        this.animalType = animalType;
        this.text = text;
        isNonNumeric = true;
    }

    public ReferenceRange(AnimalType animalType, double min, double max) {
        this.animalType = animalType;
        this.min = min;
        this.max = max;
        this.text = min + " - " + max;
        this.mean = (min + max) / 2;
    }

    public void defineText(){
        this.text = min + " - " + max;
    }

    public ReferenceRange(boolean isNonNumeric) {
        this.isNonNumeric = isNonNumeric;
    }


}
