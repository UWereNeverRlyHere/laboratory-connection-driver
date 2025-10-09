
package ywh.services.data.mapping;

import ywh.commons.ConsoleUtil;
import ywh.commons.DateTime;
import ywh.repository.analysis.entities.Indicator;
import ywh.repository.analysis.entities.ReferenceRange;
import ywh.repository.analysis.repos.IndicatorOrderRepository;
import ywh.repository.analysis.repos.IndicatorRepository;
import ywh.repository.analysis.repos.RepositoryProvider;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.observation.Deviation;
import ywh.services.data.models.observation.DeviationType;
import ywh.services.data.models.observation.PrintIndicatorResultModel;
import ywh.services.data.models.observation.ObservationData;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class LocalObservationMapper {
    private static final IndicatorRepository indicatorRepository = RepositoryProvider.indicators();
    public static final IndicatorOrderRepository indicatorOrderRepository = RepositoryProvider.indicatorOrder();

    private LocalObservationMapper() {
    }
    // Record для представлення індикатора з його значенням


    public static List<PrintIndicatorResultModel> map(ObservationData observationData) {
        List<PrintIndicatorResultModel> indicatorResultModels = new ArrayList<>();
        observationData.getData().forEach((name, value) -> {
            try {
                indicatorRepository.findByVariation(name).ifPresent(indicator -> {
                    Deviation deviationType = defineDeviation(indicator, value, observationData.getAnimalType());
                    indicatorResultModels.add(new PrintIndicatorResultModel(indicator, value, deviationType));
                });
            } catch (Exception e) {
                ConsoleUtil.printRed("Error while getting indicator by name " + name);
            }
        });
        sortImages(observationData);
        return sortIndicatorValuesByOrder(indicatorResultModels);
    }
    private static final String[] IMAGE_PRIORITY = {"wbc", "eos", "bas", "neu", "lym", "mon", "rbc", "plt"};
    private static void sortImages(ObservationData observationData){
        Map<String, String> images = observationData.getImages();
        if (images.isEmpty()) return;

        // сортуємо Entry-список
        var sorted = images.entrySet()
                .stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, ?> e) -> rank(e.getKey()))
                        .thenComparing(e -> e.getKey().toLowerCase()))
                .toList();

        // переносимо у LinkedHashMap, щоб зберегти порядок
        Map<String, String> ordered = new LinkedHashMap<>();
        sorted.forEach(e -> ordered.put(e.getKey(), e.getValue()));

        // оновлюємо Map усередині ObservationData
        observationData.getImages().clear();
        observationData.getImages().putAll(ordered);

    }
    private static int rank(String key) {
        String k = key.toLowerCase();
        for (int i = 0; i < IMAGE_PRIORITY.length; i++) {
            if (k.contains(IMAGE_PRIORITY[i])) return i;
        }
        return IMAGE_PRIORITY.length; // без збігів – у хвіст
    }

    private static List<PrintIndicatorResultModel> sortIndicatorValuesByOrder(List<PrintIndicatorResultModel> indicatorResultModels) {
        return indicatorResultModels.stream()
                .sorted((a, b) -> {
                    int placeA = indicatorOrderRepository.getPlaceForIndicator(a.indicator().getCode());
                    int placeB = indicatorOrderRepository.getPlaceForIndicator(b.indicator().getCode());

                    // Порівнюємо позиції (Integer.MAX_VALUE для тих, що не мають порядку)
                    int result = Integer.compare(placeA, placeB);

                    // Якщо позиції однакові (наприклад, обидва MAX_VALUE) - сортуємо за кодом
                    return result != 0 ? result : a.indicator().getCode().compareTo(b.indicator().getCode());
                }).toList();
    }

    private record ValueCheck(boolean isInRange, boolean isNumericValue, Double doubleValue, String textValue) {
    }

    private static Deviation defineDeviation(Indicator indicator, String value, AnimalType animalType) {

        AtomicReference<Deviation> deviation = new AtomicReference<>(new Deviation(DeviationType.UNDEFINED));
        indicator.getReferenceRange(animalType).ifPresent(
                range -> {
                    var valueCheck = isValueInRange(range, value);
                    if (valueCheck.isInRange()) {
                        deviation.set(new Deviation(DeviationType.NORMAL));
                    } else if (valueCheck.isNumericValue()) {
                        deviation.set(calculateDeviation(range, valueCheck.doubleValue()));
                    }
                }
        );
        return deviation.get();
    }


    private static Deviation calculateDeviation(ReferenceRange range, Double value) {
        DeviationType deviationType = DeviationType.NORMAL;
        String deviationText = deviationType.getDefaultText();
        double percentage;

        if (value > range.getMax()) {
            // Значення вище норми
            deviationType = DeviationType.UPPER;
            percentage = ((value - range.getMax()) / range.getMax()) * 100;
            deviationText = String.format("▲%.1f%%", percentage);
        } else if (value < range.getMin()) {
            // Значення нижче норми
            deviationType = DeviationType.LOWER;
            percentage = ((range.getMin() - value) / range.getMin()) * 100;
            deviationText = String.format("▼%.1f%%", percentage);
        }
        return new Deviation(deviationType, deviationText);
    }


    private static ValueCheck isValueInRange(ReferenceRange range, String value) {
        boolean isInRange;
        boolean isNumericValue = false;
        Double doubleValue = null;
        try {
            doubleValue = Double.parseDouble(value);
            isInRange = doubleValue >= range.getMin() && doubleValue <= range.getMax();
            isNumericValue = true;
        } catch (NumberFormatException e) {
            isInRange = range.getText().equalsIgnoreCase(value);
        }
        return new ValueCheck(isInRange, isNumericValue, doubleValue, value);
    }

    public static String getFileName(ObservationData observationData) {
        String animalName = observationData.getValue(ObservationKey.ANIMAL_NAME).orElse("unknown");
        return "%s-%s".formatted(DateTime.getDate("yyyyMMddHHmmss"), animalName);
    }

    public static String getDbfName(ObservationData observationData) {
        String idStr = observationData.getValue(ObservationKey.ID).orElse("unknown");
        return "%s-%s".formatted(DateTime.getDate("yyyyMMddHHmmss"), idStr);
    }
}