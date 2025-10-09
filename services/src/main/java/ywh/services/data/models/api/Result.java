package ywh.services.data.models.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    public String id;
    public String dateTime;

    public String deviceName;
    public String deviceSerialNumber;

    public int indicatorsCount;
    public int imagesCount;

    public List<String> alerts;

    public List<IndicatorResult> indicators;
    public List<ImageResult> images;

    public Result setIndicators(List<IndicatorResult> indicators) {
        this.indicators = indicators;
        this.indicatorsCount = indicators.size();
        return this;
    }
    public Result setImages(List<ImageResult> images) {
        this.images = images;
        this.imagesCount = images.size();
        return this;
    }
}
