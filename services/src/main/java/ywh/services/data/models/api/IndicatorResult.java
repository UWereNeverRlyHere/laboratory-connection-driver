package ywh.services.data.models.api;

import lombok.Data;
import ywh.commons.NumUtils;
import ywh.services.data.models.observation.ReferenceRangeResultModel;

import java.util.Optional;

@Data
public class IndicatorResult {

    private String code;
    private String rawCode;
    private String value;
    private String type;
    private ReferenceRangeResultModel referenceRange;

    public IndicatorResult(String code, String rawCode, String value) {
        this.code = code;
        this.rawCode = rawCode;
        this.value = value;
        defineType();

    }

    public void setValue(String value) {
        this.value = value;
        defineType();
    }

    public void defineType() {
        try {
            Optional<Double> v = NumUtils.parseDouble(value);
            if (v.isPresent()){
                value = value.replace(",", ".");
                type = value.contains(",") || value.contains(".") ? "Decimal" :  "Integer";

            }
        } catch (NumberFormatException e) {
            type = "String";
        }
    }
}
