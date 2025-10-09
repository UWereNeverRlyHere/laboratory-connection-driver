package ywh.services.data.models.observation;

public record Deviation (DeviationType type, String text){
    public Deviation(DeviationType type) {
        this(type, type.getDefaultText());
    }
};
