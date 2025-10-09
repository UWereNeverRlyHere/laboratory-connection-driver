package ywh.repository.analysis.repos.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import ywh.commons.ConsoleUtil;
import ywh.repository.analysis.repos.IndicatorOrderRepository;
import ywh.repository.repo_exceptions.LoadException;
import ywh.repository.repo_exceptions.SaveException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonIndicatorOrderRepositoryImpl implements IndicatorOrderRepository {

    private static final String REPOSITORY_FOLDER = "jsonRepository";
    private static final String ORDER_FILENAME = "indicators-order.json";

    @Getter
    private final Path directoryPath;
    @Getter
    private final Path orderFilePath;
    private final Gson gson;

    // Кеш: IndicatorCode -> Place
    private final Map<String, Integer> orderCache = new ConcurrentHashMap<>();

    public JsonIndicatorOrderRepositoryImpl() throws LoadException {
        this(getDefaultDirectoryPath());
    }

    public JsonIndicatorOrderRepositoryImpl(Path directoryPath) throws LoadException {
        this.directoryPath = directoryPath;
        this.orderFilePath = directoryPath.resolve(ORDER_FILENAME);
        this.gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        ensureDirectoryExists();
        loadData();
    }

    @Override
    public int getPlaceForIndicator(String indicatorCode) {
        return orderCache.getOrDefault(indicatorCode, Integer.MAX_VALUE);
    }

    @Override
    public void setPlaceForIndicator(int place, String indicatorCode) throws IllegalArgumentException{
        if (indicatorCode == null || indicatorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Код індикатора не може бути null або порожнім");
        }
        orderCache.put(indicatorCode, place);
    }

    @Override
    public boolean hasOrderForCode(int place, String indicatorCode) {
        Integer existingPlace = orderCache.get(indicatorCode);
        return existingPlace != null && existingPlace == place;
    }

    @Override
    public void reload() throws LoadException {
        orderCache.clear();
        loadData();
    }

    @Override
    public void saveData() throws SaveException {
        try (FileWriter writer = new FileWriter(orderFilePath.toFile())) {
            gson.toJson(orderCache, writer);
            ConsoleUtil.printMagenta("Збережено порядок " + orderCache.size() + " індикаторів у файл: " + orderFilePath);
            writer.flush();
        } catch (IOException e) {
            throw new SaveException("Не вдалось зберегти порядок індикаторів: " + orderFilePath);
        }
    }

    // Додаткові корисні методи
    public boolean hasOrderForIndicator(String indicatorCode) {
        return orderCache.containsKey(indicatorCode);
    }

    public void removeIndicator(String indicatorCode) {
        orderCache.remove(indicatorCode);
    }

    public void clearAll() {
        orderCache.clear();
    }

    public int getOrderedIndicatorsCount() {
        return orderCache.size();
    }

    private void loadData() throws LoadException {
        if (!Files.exists(orderFilePath)) {
            ConsoleUtil.printYellow("Файл порядку індикаторів не знайдено: " + orderFilePath);
            return;
        }

        try (FileReader reader = new FileReader(orderFilePath.toFile())) {
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> loadedData = gson.fromJson(reader, type);

            if (loadedData != null) {
                orderCache.clear();
                orderCache.putAll(loadedData);
                ConsoleUtil.printGreen("Завантажено порядок для " + orderCache.size() +
                        " індикаторів з файлу: " + orderFilePath);
            }
        } catch (IOException e) {
            throw new LoadException("Помилка завантаження порядку індикаторів: " + orderFilePath);
        }
    }

    private void ensureDirectoryExists() throws LoadException {
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            throw new LoadException("Не вдалось створити директорію: " + directoryPath);
        }
    }

    private static Path getDefaultDirectoryPath() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, REPOSITORY_FOLDER);
    }
}
