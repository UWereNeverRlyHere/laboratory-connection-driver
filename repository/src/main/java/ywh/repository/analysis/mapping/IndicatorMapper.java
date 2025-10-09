package ywh.repository.analysis.mapping;

import org.mapstruct.*;
import ywh.commons.TextUtils;
import ywh.repository.analysis.models.AnimalIndicatorDto;
import ywh.repository.analysis.entities.Indicator;
import ywh.repository.analysis.entities.ReferenceRange;
import ywh.repository.analysis.entities.Unit;
import ywh.repository.animals.enteties.AnimalType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "default",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IndicatorMapper {

    // Замість автоматичного маппінгу - використовуємо кастомний метод
    default Indicator toEntity(AnimalIndicatorDto.IndicatorDto dto) {
        if (dto == null) {
            return null;
        }

        Indicator indicator = new Indicator();
        indicator.setName(dto.getName());
        indicator.setCode(dto.getCode());
        indicator.setPrintName(dto.getPrintName());

        // Колекції вже ініціалізовані в конструкторі
        return indicator;
    }

    // Маппінг з Entity в DTO (ігноруємо референсний діапазон, додаємо окремо)
    @Mapping(target = "referenceRange", ignore = true)
    AnimalIndicatorDto.IndicatorDto toDto(Indicator entity);

    // Кастомний маппінг для ReferenceRange -> ReferenceRangeDto
    default AnimalIndicatorDto.ReferenceRangeDto toDto(ReferenceRange entity) {
        if (entity == null) {
            return null;
        }

        AnimalIndicatorDto.ReferenceRangeDto dto = new AnimalIndicatorDto.ReferenceRangeDto();
        dto.setMin(entity.getMin());
        dto.setMax(entity.getMax());
        dto.setMean(entity.getMean());
        dto.setText(entity.getText());
        dto.setNonNumeric(entity.isNonNumeric());

        // Конвертуємо Unit в String
        if (entity.getUnit() != null) {
            dto.setUnit(entity.getUnit().getShortName());
        } else {
            dto.setUnit("");
        }

        return dto;
    }

    // Кастомний маппінг для ReferenceRangeDto -> ReferenceRange
    default ReferenceRange toEntity(AnimalIndicatorDto.ReferenceRangeDto dto) {
        if (dto == null) {
            return null;
        }

        ReferenceRange entity = new ReferenceRange();
        entity.setMin(dto.getMin());
        entity.setMax(dto.getMax());
        entity.setMean(dto.getMean());
        if (TextUtils.isNullOrEmpty(dto.getText()))
            entity.defineText();
        else
            entity.setText(dto.getText());
        entity.setNonNumeric(dto.isNonNumeric());

        // Конвертуємо String в Unit
        if (dto.getUnit() != null) {
            try {
                entity.setUnit(Unit.findByShortName(dto.getUnit()));
            } catch (IllegalArgumentException e) {
                // Логування помилки або встановлення дефолтного значення
                entity.setUnit(Unit.DIMENSIONLESS);
            }
        } else {
            entity.setUnit(Unit.DIMENSIONLESS);
        }

        return entity;
    }

    // Додаємо новий метод для оновлення існуючого індикатора
    default void updateIndicator(Indicator existing, AnimalIndicatorDto.IndicatorDto dto) {
        if (dto == null || existing == null) {
            return;
        }

        // Оновлюємо тільки непусті поля
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            existing.setName(dto.getName());
        }

        if (dto.getPrintName() != null && !dto.getPrintName().trim().isEmpty()) {
            existing.setPrintName(dto.getPrintName());
        }

        // Код не оновлюємо, оскільки він є ключем
    }

    // Додаємо метод для оновлення референсного діапазону
    default void updateReferenceRange(ReferenceRange existing, AnimalIndicatorDto.ReferenceRangeDto dto) {
        if (dto == null || existing == null) {
            return;
        }

        // Оновлюємо числові поля (0.0 вважаємо валідним значенням)
        if (dto.getMin() != 0.0 || dto.getMax() != 0.0) {
            existing.setMin(dto.getMin());
            existing.setMax(dto.getMax());
            existing.setMean(dto.getMean());
        }

        // Оновлюємо текстові поля, якщо вони непусті
        if (dto.getText() != null && !dto.getText().trim().isEmpty()) {
            existing.setText(dto.getText());
        }

        if (dto.getUnit() != null && !dto.getUnit().trim().isEmpty()) {
            try {
                existing.setUnit(Unit.findByShortName(dto.getUnit()));
            } catch (IllegalArgumentException e) {
                existing.setUnit(Unit.DIMENSIONLESS);
            }
        }

        // Оновлюємо boolean поле
        existing.setNonNumeric(dto.isNonNumeric());
    }

    // Решта методів залишаються такими самими...
    default AnimalIndicatorDto toAnimalDto(AnimalType animalType, List<Indicator> indicators) {
        AnimalIndicatorDto dto = new AnimalIndicatorDto();
        dto.setAnimalType(animalType);
        dto.setDisplayName(animalType.getUaDefaultName());

        List<AnimalIndicatorDto.IndicatorDto> indicatorDtos = indicators.stream()
                .filter(indicator -> indicator.hasReferenceRange(animalType))
                .map(indicator -> {
                    AnimalIndicatorDto.IndicatorDto indicatorDto = toDto(indicator);

                    // Додаємо референсний діапазон для цієї тварини
                    indicator.getReferenceRange(animalType).ifPresent(range -> {
                        indicatorDto.setReferenceRange(toDto(range));
                    });

                    return indicatorDto;
                })
                .collect(Collectors.toList());

        dto.setIndicators(indicatorDtos);
        return dto;
    }

    default List<Indicator> fromAnimalDto(AnimalIndicatorDto dto) {
        return dto.getIndicators().stream()
                .map(indicatorDto -> {
                    Indicator indicator = toEntity(indicatorDto);

                    // Додаємо референсний діапазон
                    if (indicatorDto.getReferenceRange() != null) {
                        ReferenceRange range = toEntity(indicatorDto.getReferenceRange());
                        range.setAnimalType(dto.getAnimalType());
                        indicator.addReferenceRange(dto.getAnimalType(), range);
                    }

                    return indicator;
                })
                .collect(Collectors.toList());
    }

    default void mergeIndicators(List<Indicator> existingIndicators, List<Indicator> newIndicators) {
        Map<String, Indicator> existingMap = existingIndicators.stream()
                .collect(Collectors.toMap(Indicator::getCode, indicator -> indicator));

        for (Indicator newIndicator : newIndicators) {
            Indicator existing = existingMap.get(newIndicator.getCode());
            if (existing != null) {
                // Об'єднуємо референсні діапазони
                newIndicator.getReferenceRanges().forEach(existing::addReferenceRange);

                // Оновлюємо метадані якщо потрібно
                if (existing.getName() == null || existing.getName().isEmpty()) {
                    existing.setName(newIndicator.getName());
                }
                if (existing.getPrintName() == null || existing.getPrintName().isEmpty()) {
                    existing.setPrintName(newIndicator.getPrintName());
                }
            } else {
                existingIndicators.add(newIndicator);
            }
        }
    }
}