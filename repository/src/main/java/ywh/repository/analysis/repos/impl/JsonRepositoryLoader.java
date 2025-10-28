package ywh.repository.analysis.repos.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mapstruct.factory.Mappers;
import ywh.commons.ConsoleUtil;
import ywh.repository.analysis.models.AnimalIndicatorDto;
import ywh.repository.analysis.entities.Indicator;
import ywh.repository.analysis.mapping.IndicatorMapper;
import ywh.repository.analysis.entities.ReferenceRange;
import ywh.repository.repo_exceptions.LoadException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonRepositoryLoader {
    private final JsonIndicatorRepositoryImpl repository;
    private final Gson gson;
    private final IndicatorMapper mapper;

    protected JsonRepositoryLoader(JsonIndicatorRepositoryImpl repository) {
        this.repository = repository;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        this.mapper = Mappers.getMapper(IndicatorMapper.class);


    }


    // Основна логіка завантаження даних
    protected void loadData() throws LoadException {
        // Якщо indicators.json існує - просто завантажуємо його
        if (Files.exists(repository.getIndicatorsFilePath())) {
            loadFromIndicatorsFile();
        } else {
            // Якщо indicators.json не існує - шукаємо AnimalIndicatorDto файли
            ConsoleUtil.printYellow("Файл indicators.json не знайдено, пошук AnimalIndicatorDto файлів...");
            loadFromAnimalIndicatorFiles();
        }
    }

    // Завантаження з основного файлу indicators.json
    private void loadFromIndicatorsFile() throws LoadException {
        ConsoleUtil.printCyan("Завантаження з файлу: " + repository.getIndicatorsFilePath());

        try (FileReader reader = new FileReader(repository.getIndicatorsFilePath().toFile())) {
            Indicator[] indicators = gson.fromJson(reader, Indicator[].class);

            if (indicators != null) {
                for (Indicator indicator : indicators) {
                    if (indicator != null && indicator.getCode() != null) {
                        repository.getCache().put(indicator.getCode(), indicator);
                    }
                }
                ConsoleUtil.printGreen("Завантажено " + repository.getCache().size() + " показників з файлу indicators.json");
            }
        } catch (IOException e) {
            throw new LoadException("Не вдалось завантажити дані з файлу: " + repository.getIndicatorsFilePath());
        } catch (Exception e) {
            throw new LoadException("Помилка парсингу JSON: " + e.getMessage());
        }
    }

    // Пошук та завантаження з AnimalIndicatorDto файлів
    public void loadFromAnimalIndicatorFiles() throws LoadException {
        List<Path> animalFiles = findAnimalIndicatorFiles();

        if (animalFiles.isEmpty()) {
            ConsoleUtil.printYellow("AnimalIndicatorDto файли не знайдено");
            return;
        }

        // Якщо indicators.json існує, спочатку завантажуємо його
        boolean indicatorsFileExists = Files.exists(repository.getIndicatorsFilePath());
        if (indicatorsFileExists) {
            loadFromIndicatorsFile();
            ConsoleUtil.printCyan("Основний файл indicators.json завантажено. Оновлюємо дані з AnimalIndicatorDto файлів...");
        } else {
            ConsoleUtil.printYellow("indicators.json не існує. Створюємо нові дані з AnimalIndicatorDto файлів...");
        }

        // Обробляємо кожен файл AnimalIndicatorDto
        for (Path animalFile : animalFiles) {
            try {
                ConsoleUtil.printCyan("Спроба завантажити: " + animalFile);
                updateFromAnimalIndicatorFile(animalFile);
            } catch (Exception e) {
                ConsoleUtil.printRed("Помилка завантаження файлу " + animalFile.getFileName() + ": " + e.getMessage());
                // Не кидаємо виключення, просто пропускаємо файл
            }
        }

        ConsoleUtil.printGreen("Оновлено дані з " + animalFiles.size() + " AnimalIndicatorDto файлів. Всього показників: " + repository.getCache().size());

        // Зберігаємо об'єднані дані в indicators.json
        if (!repository.getCache().isEmpty()) {
            repository.saveData();
        }
    }

    // Пошук всіх потенційних AnimalIndicatorDto файлів у директорії та піддиректоріях
    private List<Path> findAnimalIndicatorFiles() throws LoadException {
        List<Path> animalFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(repository.getDirectoryPath())) {
            animalFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> !path.equals(repository.getIndicatorsFilePath())) // Виключаємо indicators.json
                    .collect(Collectors.toList());

            ConsoleUtil.printBlue("Знайдено " + animalFiles.size() + " потенційних JSON файлів");

        } catch (IOException e) {
            throw new LoadException("Помилка пошуку файлів у директорії: " + repository.getDirectoryPath());
        }

        return animalFiles;
    }

    // Спроба завантажити файл як AnimalIndicatorDto
    private List<Indicator> loadAnimalIndicatorFile(Path animalFile) throws IOException {
        try (FileReader reader = new FileReader(animalFile.toFile())) {
            // Спробуємо розпарсити як AnimalIndicatorDto
            AnimalIndicatorDto animalDto = gson.fromJson(reader, AnimalIndicatorDto.class);

            // Перевіряємо, чи це валідний AnimalIndicatorDto
            if (animalDto != null &&
                    animalDto.getAnimalType() != null &&
                    animalDto.getIndicators() != null &&
                    !animalDto.getIndicators().isEmpty()) {

                ConsoleUtil.printGreen("  ✓ Файл " + animalFile.getFileName() + " є валідним AnimalIndicatorDto для " + animalDto.getAnimalType());
                return mapper.fromAnimalDto(animalDto);
            } else {
                ConsoleUtil.printYellow("  ✗ Файл " + animalFile.getFileName() + " не є валідним AnimalIndicatorDto");
                return new ArrayList<>();
            }
        }
    }

    // Новий метод для оновлення даних з AnimalIndicatorDto файлу
    private void updateFromAnimalIndicatorFile(Path animalFile) throws IOException {
        try (FileReader reader = new FileReader(animalFile.toFile())) {
            // Спробуємо розпарсити як AnimalIndicatorDto
            AnimalIndicatorDto animalDto = gson.fromJson(reader, AnimalIndicatorDto.class);

            // Перевіряємо, чи це валідний AnimalIndicatorDto
            if (animalDto != null &&
                    animalDto.getAnimalType() != null &&
                    animalDto.getIndicators() != null &&
                    !animalDto.getIndicators().isEmpty()) {

                ConsoleUtil.printGreen("  ✓ Файл " + animalFile.getFileName() + " є валідним AnimalIndicatorDto для " + animalDto.getAnimalType());

                // Оновлюємо або додаємо індикатори
                updateIndicatorsFromDto(animalDto);
            } else {
                ConsoleUtil.printYellow("  ✗ Файл " + animalFile.getFileName() + " не є валідним AnimalIndicatorDto");
            }
        }
    }

    // Метод для оновлення індикаторів з DTO
// Метод для оновлення індикаторів з DTO
    private void updateIndicatorsFromDto(AnimalIndicatorDto animalDto) {
        for (AnimalIndicatorDto.IndicatorDto indicatorDto : animalDto.getIndicators()) {
            String code = indicatorDto.getCode();
            if (code == null || code.trim().isEmpty()) {
                continue; // Пропускаємо індикатори без коду
            }

            // Знаходимо існуючий індикатор або створюємо новий
            Indicator existingIndicator = repository.getCache().get(code);
            if (existingIndicator == null) {
                // Створюємо новий індикатор ТІЛЬКИ якщо його не існує
                existingIndicator = mapper.toEntity(indicatorDto);
                repository.getCache().put(code, existingIndicator);
                ConsoleUtil.printBlue("    + Створено новий індикатор: " + code);
            } else {
                // ВАЖЛИВО: Оновлюємо існуючий індикатор БЕЗ заміни analyzerCodes/orderCodes
                updateIndicatorFromDto(existingIndicator, indicatorDto);
                ConsoleUtil.printBlue("    ↻ Оновлено індикатор: " + code +
                        " (зберігаємо analyzerCodes: " + existingIndicator.getVariations().size() +
                        ", orderCodes: " + existingIndicator.getOrderCodes().size() + ")");
            }

            // Додаємо або оновлюємо референсний діапазон для тварини
            if (indicatorDto.getReferenceRange() != null) {
                ReferenceRange range = mapper.toEntity(indicatorDto.getReferenceRange());
                range.setAnimalType(animalDto.getAnimalType());
                existingIndicator.addReferenceRange(animalDto.getAnimalType(), range);
                ConsoleUtil.printBlue("      → Додано/оновлено діапазон для " + animalDto.getAnimalType());
            }
        }
    }


    // Метод для оновлення полів індикатора з DTO (тільки непусті поля)
    private void updateIndicatorFromDto(Indicator existing, AnimalIndicatorDto.IndicatorDto dto) {
        boolean updated = false;

        // Оновлюємо name, якщо воно непусте і відрізняється
        if (dto.getName() != null && !dto.getName().trim().isEmpty() &&
                !dto.getName().equals(existing.getName())) {
            existing.setName(dto.getName());
            updated = true;
        }

        // Оновлюємо printName, якщо воно непусте і відрізняється
        if (dto.getPrintName() != null && !dto.getPrintName().trim().isEmpty() &&
                !dto.getPrintName().equals(existing.getPrintName())) {
            existing.setPrintName(dto.getPrintName());
            updated = true;
        }

        // ВАЖЛИВО: НЕ чіпаємо analyzerCodes та orderCodes!
        // Ці списки мають зберігатися незмінними, оскільки вони заповнюються з інших джерел

        if (updated) {
            ConsoleUtil.printBlue("        ✓ Оновлено метадані індикатора " + existing.getCode() +
                    " (analyzerCodes: " + existing.getVariations().size() +
                    ", orderCodes: " + existing.getOrderCodes().size() + ")");
        }
    }

}