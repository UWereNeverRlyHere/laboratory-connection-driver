package ywh.fx_app.clarification;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ywh.fx_app.app_data.FXMLLoaders;
import ywh.fx_app.app_data.ImageLoader;
import ywh.fx_app.app_utils.StageUtils;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.device.IClarificationProvider;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClarificationProvider implements IClarificationProvider {

    // Статичні поля для єдиного вікна на весь додаток
    private static volatile Stage clarificationStage;
    private static ClarificationContainerController containerController;
    private static final Object LOCK = new Object(); // Для thread safety

    @Override
    public CompletableFuture<ObservationData> requestClarification(ObservationData observationData) {
        return Platform.isFxApplicationThread() ?
                requestClarificationOnFxThread(observationData) :
                requestClarificationFromBackgroundThread(observationData);
    }

    /**
     * Обробка запиту на FX потоці
     */
    private static CompletableFuture<ObservationData> requestClarificationOnFxThread(ObservationData observationData) {
        try {
            ensureClarificationWindowExists();
            showWindow();
            return containerController.addClarificationRequest(observationData);

        } catch (Exception e) {
            CompletableFuture<ObservationData> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Обробка запиту з фонового потоку
     */
    private static CompletableFuture<ObservationData> requestClarificationFromBackgroundThread(ObservationData observationData) {
        CompletableFuture<ObservationData> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                CompletableFuture<ObservationData> fxThreadFuture = requestClarificationOnFxThread(observationData);
                // Перенаправляємо результат
                fxThreadFuture.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(result);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Лейзі створення вікна (thread-safe)
     */
    private static void ensureClarificationWindowExists() {
        if (clarificationStage == null) {
            synchronized (LOCK) {
                if (clarificationStage == null) { // Double-checked locking
                    createClarificationWindow();
                }
            }
        }
    }

    /**
     * Створює головне вікно уточнення
     */
    private static void createClarificationWindow() {
        try {
            // Завантажуємо FXML
            FXMLLoader loader = FXMLLoaders.CLARIFICATION_CONTAINER.getLoader();
            VBox root = loader.load();
            containerController = loader.getController();

            // Створюємо Stage
            clarificationStage = new Stage();
            clarificationStage.setTitle("Уточнення даних");
            clarificationStage.initModality(Modality.NONE);
            clarificationStage.initStyle(StageStyle.DECORATED);
            clarificationStage.setResizable(true);

            // Налаштовуємо сцену
            Scene scene = new Scene(root);
            clarificationStage.setScene(scene);
            clarificationStage.getIcons().add(ImageLoader.APP_IMAGE.getFxImage());

            // Центруємо вікно
            StageUtils.setMinStageBoundsByScreenSize(clarificationStage, 0.1, 0.15);
            StageUtils.setDefaultStageBounds(clarificationStage, 0.3, 0.6);

            // Обробляємо закриття вікна
            clarificationStage.setOnCloseRequest(event -> {
                // Приховуємо замість закриття, щоб зберегти ресурси
                event.consume();
                clarificationStage.setIconified(true);
            });

        } catch (IOException e) {
            throw new RuntimeException("Failed to createApi clarification window", e);
        }
    }

    /**
     * Показує вікно уточнення
     */
    private static void showWindow() {
        if (clarificationStage != null) {
            if (!clarificationStage.isShowing()) {
                clarificationStage.show();
                clarificationStage.setIconified(false);
            }
            if (clarificationStage.isIconified()) {
                clarificationStage.setIconified(false);
            }
            clarificationStage.toFront();
            clarificationStage.requestFocus();
        }
    }

    /**
     * Статичний метод для отримання екземпляра контейнера (якщо потрібно)
     */
    public static ClarificationContainerController getContainerController() {
        ensureClarificationWindowExists();
        return containerController;
    }

    /**
     * Статичний метод для прямого додавання запиту (альтернативний спосіб використання)
     */
    public static CompletableFuture<ObservationData> addClarificationRequest(ObservationData observationData) {
        return Platform.isFxApplicationThread() ?
                requestClarificationOnFxThread(observationData) :
                requestClarificationFromBackgroundThread(observationData);
    }

    /**
     * Закриває та очищує ресурси (опціонально для cleanup)
     */
    public static void shutdown() {
        Platform.runLater(() -> {
            synchronized (LOCK) {
                if (clarificationStage != null) {
                    clarificationStage.close();
                    clarificationStage = null;
                    containerController = null;
                }
            }
        });
    }

    /**
     * Перевіряє, чи існує вікно
     */
    public static boolean isWindowCreated() {
        return clarificationStage != null;
    }

    /**
     * Перевіряє, чи показане вікно
     */
    public static boolean isWindowShowing() {
        return clarificationStage != null && clarificationStage.isShowing();
    }
}