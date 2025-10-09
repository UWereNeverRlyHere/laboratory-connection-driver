package ywh.services.device;

import ywh.services.data.models.observation.ObservationData;

import java.util.concurrent.CompletableFuture;

public interface IClarificationProvider {
    CompletableFuture<ObservationData> requestClarification(ObservationData observationData);

}
