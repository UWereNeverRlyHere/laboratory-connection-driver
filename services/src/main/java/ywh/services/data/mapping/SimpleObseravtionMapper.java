package ywh.services.data.mapping;

import ywh.repository.analysis.repos.IndicatorRepository;
import ywh.repository.analysis.repos.RepositoryProvider;
import ywh.services.data.models.observation.ObservationData;

import java.util.HashMap;

public class SimpleObseravtionMapper {
    private SimpleObseravtionMapper() {
    }

    private static final IndicatorRepository indicatorRepository = RepositoryProvider.indicators();

    public static void map(ObservationData observationData) {
        var mappedData = new HashMap<String, String>();
        observationData.getData().forEach((key, value) -> indicatorRepository
                .findByCode(key)
                .ifPresentOrElse(indicator -> {
                            if (indicator.getVariations().isEmpty())
                                mappedData.put(key, value);
                            else
                                indicator.getVariations()
                                        .forEach(variation -> mappedData.put(variation, value));
                        },
                        () -> mappedData.put(key, value)));
        observationData.getData().clear();
        observationData.getData().putAll(mappedData);
    }
}
