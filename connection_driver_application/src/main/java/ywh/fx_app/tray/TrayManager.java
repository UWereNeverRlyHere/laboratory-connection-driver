package ywh.fx_app.tray;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import ywh.logging.MainLogger;
import ywh.fx_app.app_data.FXMLLoaders;
import ywh.fx_app.app_data.ImageLoader;
import ywh.fx_app.app_utils.StageUtils;
import ywh.fx_app.application.AppStaticConfig;
import ywh.fx_app.application.DriverApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class TrayManager {
    private TrayManager() {
    }

    private static TrayIcon trayIcon;
    private static Stage popupStage;
    @Getter
    private static TrayController controller;
    private static Stage primaryStage;
    private static Runnable closeAction;

    public static final AtomicInteger X = new AtomicInteger(0);
    public static final AtomicInteger Y = new AtomicInteger(0);

    public static void createTray(Stage primaryStage, Runnable closeAction) {
        if (trayIcon != null) return;
        if (!SystemTray.isSupported()) {
            MainLogger.warn("System tray is not supported");
            return;
        }

        TrayManager.primaryStage = primaryStage;
        TrayManager.closeAction = closeAction;

        // Налаштовуємо поведінку при закритті вікна
     /*   primaryStage.setOnCloseRequest(e -> {
            e.consume(); // Не дозволяємо закритися
            primaryStage.hide(); // Тільки ховаємо
            MainLogger.info("Main window hidden to tray");
        });*/
        trayIcon = new TrayIcon(ImageLoader.APP_IMAGE.getAwtImage(), AppStaticConfig.APP_NAME, null);
        trayIcon.setImageAutoSize(true);
        createPopup();
        setUpMouseListener();

        //primaryStage.getIcons().add(ImageLoader.APP_IMAGE.getFxImage());
        popupStage.getIcons().add(ImageLoader.APP_IMAGE.getFxImage());
        popupStage.show();


        try {

            SystemTray.getSystemTray().add(trayIcon);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            DisplayMode dm = gd.getDisplayMode();
            X.set(dm.getWidth());
            Y.set(dm.getHeight());
            MainLogger.info("Tray icon created successfully");
        } catch (AWTException e) {
            MainLogger.error("Failed to add tray icon: " + e.getMessage(), e);
        }
        popupStage.hide();
    }

    private static void showPopup() {
        if (popupStage != null && !popupStage.isShowing()) {
            TrayAnimation.expand(popupStage);
            popupStage.setOpacity(0.95);

            double adjustedX = X.get();
            double adjustedY = Y.get() - popupStage.getHeight() - 20;
            popupStage.setX(adjustedX);
            popupStage.setY(adjustedY);
        }
    }

    private static void setUpMouseListener() {
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                X.set(e.getXOnScreen());
                Y.set(e.getYOnScreen());

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Перевіряємо, чи JavaFX ще працює
                    if (Platform.isFxApplicationThread() || !Platform.isImplicitExit()) {
                        Platform.runLater(TrayManager::showPopup);
                    } else {
                        // Якщо JavaFX завершився, викликаємо показ попапу безпосередньо
                        SwingUtilities.invokeLater(TrayManager::showPopup);
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    Platform.runLater(() -> {
                        if (controller != null) {
                            controller.openMainWindow();
                        }
                    });
                }
            }
        });
    }

    private static void createPopup() {
        try {
            FXMLLoader loader = FXMLLoaders.TRAY.getLoader();
            Scene scene = new Scene(loader.load());
            controller = loader.getController();
            controller.setStageAndCloseAction(primaryStage, closeAction);

            popupStage = new Stage();
            popupStage.initOwner(primaryStage);
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.setAlwaysOnTop(true);
            popupStage.setResizable(true);
            primaryStage.getIcons().clear();
            primaryStage.getIcons().addFirst(ImageLoader.APP_IMAGE.getFxImage());

            popupStage.getScene().getStylesheets().addFirst(String.valueOf(DriverApp.class.getResource("/css/tray.css")));
            StageUtils.setUpRoundCorners(scene, popupStage);
            StageUtils.setMinStageBoundsByScreenSize(popupStage, 0.08, 0.08);
            TrayAnimation.minimize(popupStage);
            popupStage.show();

            popupStage.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.FALSE.equals(newVal)) {
                    TrayAnimation.minimize(popupStage);
                }
            });

        } catch (Exception e) {
            MainLogger.error("Failed to createApi popup: " + e.getMessage(), e);
        }
    }

    /**
     * Видаляє трей з системи
     */
    public static void removeTray() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                MainLogger.info("Tray icon removed");
            } catch (Exception e) {
                MainLogger.error("Failed to remove tray icon: " + e.getMessage(), e);
            }
            trayIcon = null;
        }

        if (popupStage != null) {
            popupStage.hide();
            popupStage = null;
        }

        controller = null;
        primaryStage = null;
        closeAction = null;
    }

}