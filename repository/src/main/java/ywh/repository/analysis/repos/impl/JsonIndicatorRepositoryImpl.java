package ywh.repository.analysis.repos.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.mapstruct.factory.Mappers;
import ywh.commons.ConsoleUtil;
import ywh.repository.analysis.models.AnimalIndicatorDto;
import ywh.repository.analysis.entities.Indicator;
import ywh.repository.analysis.mapping.IndicatorMapper;
import ywh.repository.analysis.repos.IndicatorRepository;
import ywh.repository.animals.enteties.AnimalType;
import ywh.repository.repo_exceptions.LoadException;
import ywh.repository.repo_exceptions.NonUniqEntity;
import ywh.repository.repo_exceptions.NotFoundException;
import ywh.repository.repo_exceptions.SaveException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JsonIndicatorRepositoryImpl implements IndicatorRepository {
    private static final String REPOSITORY_FOLDER = "jsonRepository";
    private static final String DEFAULT_FILENAME = "indicators.json";
    @Getter
    private final Path directoryPath;
    @Getter
    private final Path indicatorsFilePath;
    private final Gson gson;
    private final IndicatorMapper mapper;
    @Getter
    private final Map<String, Indicator> cache = new HashMap<>();
    private final JsonRepositoryLoader loader;

    public JsonIndicatorRepositoryImpl() throws LoadException {
        this(getDefaultDirectoryPath());
    }

    public JsonIndicatorRepositoryImpl(Path directoryPath) throws LoadException {
        this.directoryPath = directoryPath;
        this.indicatorsFilePath = directoryPath.resolve(DEFAULT_FILENAME);
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        this.mapper = Mappers.getMapper(IndicatorMapper.class);
        loader = new JsonRepositoryLoader(this);
        ensureDirectoryExists();

        loader.loadData();
    }


    // Експорт даних в окремі файли тварин
    public void exportToAnimalFiles() throws SaveException {
        for (AnimalType animalType : AnimalType.values()) {
            if (animalType == AnimalType.OTHER) continue;

            exportAnimalFile(animalType);
        }
    }

    private void exportAnimalFile(AnimalType animalType) throws SaveException {
        String fileName = animalType.name().toLowerCase() + "s.json";
        Path animalFile = directoryPath.resolve(fileName);

        List<Indicator> indicatorsForAnimal = cache.values().stream()
                .filter(indicator -> indicator.hasReferenceRange(animalType))
                .collect(Collectors.toList());

        if (indicatorsForAnimal.isEmpty()) {
            ConsoleUtil.printYellow("Немає показників для " + animalType + ", файл не створюється");
            return;
        }

        AnimalIndicatorDto animalDto = mapper.toAnimalDto(animalType, indicatorsForAnimal);

        // Сортуємо для зручності
        animalDto.getIndicators().sort(Comparator.comparing(AnimalIndicatorDto.IndicatorDto::getCode));

        try (FileWriter writer = new FileWriter(animalFile.toFile())) {
            gson.toJson(animalDto, writer);
            ConsoleUtil.printGreen("Експортовано " + animalDto.getIndicators().size() +
                    " показників для " + animalType + " в файл: " + fileName);
        } catch (IOException e) {
            throw new SaveException("Помилка збереження файлу " + fileName);
        }
    }

    // Примусова пересинхронізація з AnimalIndicatorDto файлів
    public void forceSyncFromAnimalFiles() throws LoadException, SaveException {
        cache.clear();
        loader.loadFromAnimalIndicatorFiles();
        ConsoleUtil.printGreen("Примусова синхронізація завершена!");
    }

    // Допоміжні методи
    private void ensureDirectoryExists() throws LoadException {
        try {
            Files.createDirectories(directoryPath);
            ConsoleUtil.printMagenta("Директорія забезпечена: " + directoryPath);
        } catch (IOException e) {
            throw new LoadException("Не вдалось створити директорію: " + directoryPath);
        }
    }

    private static Path getDefaultDirectoryPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, REPOSITORY_FOLDER);
    }


    @Override
    public List<Indicator> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public Optional<Indicator> findByCode(String code) {
        return Optional.ofNullable(cache.get(code));
    }

    @Override
    public Optional<Indicator> findByVariation(String code) {
        Optional<Indicator> indicator = findByCode(code);
        return indicator.isPresent() ? indicator : cache.values()
                .stream()
                .filter(ind -> ind.getVariations().contains(code))
                .findFirst();
    }

    @Override
    public Optional<Indicator> findByName(String name) {
        return cache.values()
                .stream()
                .filter(indicator -> indicator.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<Indicator> findByAnimalType(AnimalType animalType) {
        return cache.values()
                .stream()
                .filter(indicator -> indicator.hasReferenceRange(animalType))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }


    @Override
    public boolean add(Indicator indicator) throws IllegalArgumentException {
        if (indicator == null || indicator.getCode() == null) {
            throw new IllegalArgumentException("Indicator або його код не може бути null");
        }
        if (cache.containsKey(indicator.getCode())) {
            throw new NonUniqEntity("Indicator з таким кодом вже присутній");
        }
        // Повертає true, якщо елемент був доданий (не існував)
        return cache.putIfAbsent(indicator.getCode(), indicator) == null;
    }

    @Override
    public boolean update(Indicator indicator) throws IllegalArgumentException {
        if (indicator == null || indicator.getCode() == null) {
            throw new IllegalArgumentException("Indicator або його код не може бути null");
        }
        if (!cache.containsKey(indicator.getCode())) {
            throw new NotFoundException("Indicator не знайдено");
        }
        // Повертає true, якщо елемент був оновлений (існував)
        return cache.replace(indicator.getCode(), indicator) != null;
    }


    @Override
    public void saveAll(List<Indicator> indicators) throws IllegalArgumentException {
        if (indicators == null) {
            throw new IllegalArgumentException("Список indicators не може бути null");
        }
        // Перевірка на дублікати ПЕРЕД збереженням
        Set<String> codes = new HashSet<>();
        for (Indicator indicator : indicators) {
            if (indicator != null && indicator.getCode() != null && !codes.add(indicator.getCode())) {
                throw new IllegalArgumentException("Дублікат коду в списку: " + indicator.getCode());
            }

        }
        // Тепер додаємо всі
        indicators.forEach(indicator -> {
            if (indicator != null && indicator.getCode() != null) {
                cache.put(indicator.getCode(), indicator);
            }
        });
        saveData();
    }

    @Override
    public void delete(String code) throws IllegalArgumentException {
        if (code == null) {
            throw new IllegalArgumentException("Код не може бути null");
        }
        if (!cache.containsKey(code)) {
            throw new NotFoundException("Indicator з кодом '" + code + "' не знайдено");
        }
        cache.remove(code);
    }


    @Override
    public boolean exists(String code) {
        return code != null && cache.containsKey(code);
    }

    @Override
    public void reload() throws LoadException {
        loader.loadFromAnimalIndicatorFiles();
    }


    @Override
    public void saveData() throws SaveException {
        try (FileWriter writer = new FileWriter(indicatorsFilePath.toFile())) {
            List<Indicator> indicators = new ArrayList<>(cache.values());
            // Сортуємо за кодом для зручності
            indicators.sort(Comparator.comparing(Indicator::getCode));

            gson.toJson(indicators, writer);
            ConsoleUtil.printMagenta("Збережено " + indicators.size() + " показників у файл: " + indicatorsFilePath);
            writer.flush();
        } catch (IOException e) {
            throw new SaveException("Не вдалось зберегти дані у файл: " + indicatorsFilePath);
        }
    }
}