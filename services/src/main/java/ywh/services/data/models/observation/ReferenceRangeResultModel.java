package ywh.services.data.models.observation;

import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.commons.NumUtils;
import ywh.commons.TextUtils;

@Data
@NoArgsConstructor
public class ReferenceRangeResultModel {
    Double min;
    Double mean;
    Double max;
    Double deviation;
    String text;
    String unit;
    DeviationType deviationType;
    String deviationText;


    public ReferenceRangeResultModel(String min, String max) {
        this.min = NumUtils.parseDouble(min).orElse(Double.MIN_VALUE);
        this.max = NumUtils.parseDouble(max).orElse(Double.MAX_VALUE);
    }

    public ReferenceRangeResultModel(String min, String max, String unit) {
        this.min = NumUtils.parseDouble(min).orElse(Double.MIN_VALUE);
        this.max = NumUtils.parseDouble(max).orElse(Double.MAX_VALUE);
        this.unit = unit;
    }

    public void define(String value) {
        if (TextUtils.isNullOrEmpty(this.text))
            this.text = min + " - " + max;
        this.mean = (min + max) / 2;
        defineDeviation(value);
    }


    private void defineDeviation(String value) {
        deviationType = DeviationType.NORMAL;
        deviationText = deviationType.getDefaultText();
        NumUtils.parseDouble(value, numVal -> {
            if (numVal > max) {
                // Значення вище норми
                deviationType = DeviationType.UPPER;
                deviation = ((numVal - max) / max) * 100;
                deviationText = String.format("▲%.1f%%", deviation);
            } else if (numVal < min) {
                // Значення нижче норми
                deviationType = DeviationType.LOWER;
                deviation = ((min - numVal) / min) * 100;
                deviationText = String.format("▼%.1f%%", deviation);
            }
            if(deviation!= null && (deviation == Double.POSITIVE_INFINITY || deviation == Double.NEGATIVE_INFINITY))deviation = null;
        }, () -> {
            if (!value.equals(text)) {
                deviationType = DeviationType.NOT_NORMAL;
                deviationText = deviationType.getDefaultText();

            }
        });

    }
}
