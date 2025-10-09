package ywh.services.data.models.api;

import lombok.Data;
import ywh.commons.NumUtils;

import java.util.ArrayList;
import java.util.List;
@Data
public class Order {
    private String id = "";
    private String bioMaterialTypeCode = "";
    private boolean isCito;
    private PatientData patientData = new PatientData("","","");
    private List<String> indicators = List.of();

    public PatientData getPatientData() {
        if (patientData == null) patientData = new PatientData("","","");
        return patientData;
    }

    public void distinctIndicators() {
        indicators = indicators.stream().distinct().toList();
    }

    public void removeNonIntegerIndicators() {
        indicators = indicators.stream().filter(NumUtils::isInteger).toList();
    }

    public void splitByHyphenIndicators() {
        List<String> inds = new ArrayList<>();
        indicators.forEach(ind -> {
            if (ind.contains("-")) {
                String[] split = ind.split("-");
                inds.add(split[0].trim());
                inds.add(split[1].trim());
            } else {
                inds.add(ind);
            }
        });
        indicators = inds;
    }
}
