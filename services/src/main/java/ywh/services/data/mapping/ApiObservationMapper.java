package ywh.services.data.mapping;

import ywh.commons.DateTime;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.api.ImageResult;
import ywh.services.data.models.api.IndicatorResult;
import ywh.services.data.models.api.Result;
import ywh.services.data.models.observation.ObservationData;

import java.util.ArrayList;
import java.util.List;

public class ApiObservationMapper {
    private ApiObservationMapper() {
    }

    public static Result map(ObservationData observationData, String deviceName, String deviceSerialNumber) {
        var result = Result.builder()
                .id(observationData.getId().orElse("undefined id"))
                .deviceName(deviceName)
                .deviceSerialNumber(deviceSerialNumber)
                .dateTime(observationData.getDate().orElse(DateTime.getDateTimeForJson()))
                .alerts(observationData.getAlerts())
                .build();
        List<IndicatorResult> indicators = new ArrayList<>();
        List<ImageResult> images = new ArrayList<>();
        observationData.getData().forEach((key, value) -> {
            if (ObservationKey.hasName(key)) return;
            //TODO add mapping for code
            var indicator = new IndicatorResult(key, key, value);
            var reference = observationData.getReference(key);
            reference.ifPresent(ref -> {
                ref.define(value);
                indicator.setReferenceRange(ref);
            });
            indicators.add(indicator);

        });
        observationData.getImages().forEach((key, value) -> {
            images.add(new ImageResult(key, value));
        });
        result.setIndicators(indicators);
        result.setImages(images);
        return result;

    }
}
