package ywh.services.device.parsers;

import lombok.*;
import ywh.commons.TextUtils;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.data.models.observation.ReferenceRangeResultModel;

import java.util.function.Consumer;


public class ParsingContext {
    @Getter
    @Setter
    String barcode = "";
    String id = "";
    String tempId;
    @Getter
    ObservationData observationData = new ObservationData();
    @Getter
    boolean isOrderFlag = false;
    @Setter
    byte [] ack = new byte[0];

    void reset() {
        id = "";
        tempId = "";
        barcode = "";
        observationData = new ObservationData();
        isOrderFlag = false;
    }

    void resetObservationData() {
        observationData = new ObservationData();
    }

    public void markAsOrder() {
        isOrderFlag = true;
    }

    public boolean isNotOrderFlag() {
        return !isOrderFlag;
    }

    public String getId() {
        return barcode;
    }

    public void putId(String id) {
        this.id = id;
        observationData.putId(id);
    }
    public void putTempId(String id) {
        this.tempId = id;
    }

    public void putDate(String date) {
        observationData.putDate(date);
    }

    public void putIdFromLastBarcode() {
        putId(barcode);
    }
    public void putAnimalType(String animalType) {
        observationData.putAnimalType(animalType);
    }
    public void putAnimalType(AnimalType animalType) {
        observationData.putAnimalType(animalType);
    }

    public void putOwner(String owner){
        observationData.putOwner(owner);
    }

    public void put(String indicatorName, String value) {
        observationData.put(indicatorName, value);
    }

    public void putImage(String imageName, String value) {
        observationData.putImage(imageName, value);
    }


    public void putReferences(Consumer<ReferenceBuilder> builderConsumer) {
        try {
            ReferenceBuilder builder = new ReferenceBuilder();
            builderConsumer.accept(builder);
            ReferenceRangeResultModel ref = builder.build();
            if (ref != null) {
                observationData.putReference(builder.indicatorName, ref);
            }
        } catch (Exception ignored) {
            // ignored
        }
    }



    public ObservationData getCopyAndReset() {
        var data = new ObservationData(observationData);
        if (TextUtils.isNullOrEmpty(id) && TextUtils.isNotNullOrEmpty(tempId))
            data.putId(tempId);
        reset();
        return data;
    }

    public ParsingResult getParsingResultAndReset() {
        return new ParsingResult(getCopyAndReset(), ack);
    }

    public ParsingResult getParsingResultAndReset(byte[] response) {
        return new ParsingResult(getCopyAndReset(), response);
    }

    public static class ReferenceBuilder {
        private String min;
        private String max;
        private String unit;
        private String indicatorName;

        public ReferenceBuilder indicatorName(String indicatorName) {
            this.indicatorName = indicatorName;
            return this;
        }

        public ReferenceBuilder min(String min) {
            this.min = min;
            return this;
        }

        public ReferenceBuilder max(String max) {
            this.max = max;
            return this;
        }

        public ReferenceBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public ReferenceRangeResultModel build() {
            if (min != null && max != null) {
                return new ReferenceRangeResultModel(min, max, unit);
            }
            return null;
        }

    }

}