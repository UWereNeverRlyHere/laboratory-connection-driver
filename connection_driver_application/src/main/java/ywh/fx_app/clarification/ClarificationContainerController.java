package ywh.fx_app.clarification;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ywh.fx_app.app_data.FXMLLoaders;
import ywh.fx_app.app_utils.Animations;
import ywh.services.data.models.observation.ObservationData;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class ClarificationContainerController {
    @FXML
    private ScrollPane scrollPane;
    @FXML private VBox itemsContainer;
    @FXML private MFXButton confirmAllBtn;
    @FXML private MFXButton cancelAllBtn;

    private final ConcurrentHashMap<ClarificationWindowController, CompletableFuture<ObservationData>> pendingRequests = new ConcurrentHashMap<>();
    private final Semaphore confirmationSemaphore = new Semaphore(1);



    protected CompletableFuture<ObservationData> addClarificationRequest(ObservationData data) {
        CompletableFuture<ObservationData> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            ClarificationWindowController controller = addObservationData(data);
            pendingRequests.put(controller, future);
        });
        return future;
    }

    protected void confirmSingle(ClarificationWindowController controller, ObservationData updatedData) {
        CompletableFuture.runAsync(() -> {
            try {
                confirmationSemaphore.acquire();
                Platform.runLater(() -> {
                    try {
                        CompletableFuture<ObservationData> future = remove(controller);
                        if (future != null) {
                            future.complete(updatedData);
                        }
                        if (pendingRequests.isEmpty()) {
                            closeWindow();
                        }
                    } finally {
                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                confirmationSemaphore.release();
                            }
                        });

                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                confirmationSemaphore.release();
            }
        });
    }



    private CompletableFuture<ObservationData> remove(ClarificationWindowController controller){
        Animations.fadeOut(controller.getRoot(), 300, event -> itemsContainer.getChildren().remove(controller.getRoot()));
        return pendingRequests.remove(controller);
    }

    private ClarificationWindowController addObservationData(ObservationData data) {
        try {
            FXMLLoader loader = FXMLLoaders.CLARIFICATION_WINDOW.getLoader();
            TitledPane titledPane = loader.load();
            ClarificationWindowController itemController = loader.getController();
            itemController.setUp(this, data);
            itemsContainer.getChildren().add(titledPane);
            return itemController; // ✅ Повертаємо контролер
        } catch (IOException e) {
            throw new RuntimeException("Failed to load clarification item", e);
        }
    }


    @FXML
    private void confirmAll() {
        for (var entry : pendingRequests.entrySet()) {
            ClarificationWindowController controller = entry.getKey();
            controller.confirm();
        }
    }

    @FXML
    private void cancelAll() {
        for (var entry : pendingRequests.entrySet()) {
            CompletableFuture<ObservationData> future = entry.getValue();
            future.complete(new ObservationData());
        }
        clearAndClose();
    }



    private void clearAndClose() {
        itemsContainer
                .getChildren()
                .forEach(node -> Animations.fadeOut(node, 300, event -> itemsContainer.getChildren().remove(node)));
        pendingRequests.clear();
       closeWindow();
    }
    private void closeWindow() {
        Stage stage = (Stage) confirmAllBtn.getScene().getWindow();
        stage.close();
    }

}
