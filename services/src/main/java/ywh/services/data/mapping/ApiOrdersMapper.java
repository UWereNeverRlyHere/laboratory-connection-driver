package ywh.services.data.mapping;

import ywh.logging.DeviceLogger;
import ywh.repository.analysis.entities.Indicator;
import ywh.repository.analysis.repos.IndicatorRepository;
import ywh.repository.analysis.repos.RepositoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApiOrdersMapper {
    private ApiOrdersMapper() {}

    private static final IndicatorRepository indicatorRepository = RepositoryProvider.indicators();

    public static List<String> mapIndicators(List<String> notMappedIndicators, DeviceLogger logger) {
        logger.writeSeparator();
        logger.log("Mapping indicators...");
        logger.log("Not mapped indicators: " + String.join(", ", notMappedIndicators));
        if(indicatorRepository.isEmpty()) {
            logger.log("Mapped indicators: " + String.join(", ", notMappedIndicators));
            return notMappedIndicators;
        }
        var mappedIndicators = new ArrayList<String>();
        for (String ind : notMappedIndicators) {
            try {
                Optional<Indicator> repoInd = indicatorRepository.findByVariation(ind);
                repoInd.ifPresentOrElse(indicator -> mappedIndicators.addAll(indicator.getOrderCodes()),
                        () -> mappedIndicators.add(ind));
            } catch (Exception e) {
                mappedIndicators.add(ind);
            }
        }
        logger.log("Mapped indicators: " + String.join(", ", mappedIndicators));
        logger.log("Mapping indicators finished");
        logger.writeSeparator();
        return mappedIndicators;

    }
}
