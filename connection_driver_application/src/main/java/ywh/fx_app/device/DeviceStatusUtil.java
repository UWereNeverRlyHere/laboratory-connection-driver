package ywh.fx_app.device;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import ywh.services.data.enums.DeviceStatus;
import ywh.services.device.Device;

import java.util.Map;

public class DeviceStatusUtil {

    private static final Map<DeviceStatus, Color> STATUS_COLORS = Map.of(
            DeviceStatus.WORKING, Color.GREEN,
            DeviceStatus.TRY_START, Color.ORANGE,
            DeviceStatus.STOPPED, Color.BLACK,
            DeviceStatus.ERROR, Color.RED,
            DeviceStatus.PARSING_ERROR, Color.PINK,
            DeviceStatus.TRY_PARSE, Color.CYAN,

            // НОВІ статуси підключення
            DeviceStatus.CONNECTING, Color.YELLOW,        // Жовтий - процес підключення
            DeviceStatus.CONNECTED, Color.LIGHTGREEN,     // Світло-зелений - підключено, але ще не працює
            DeviceStatus.RECONNECTING, Color.GOLD,        // Золотий - перепідключення
            DeviceStatus.CONNECTION_LOST, Color.CORAL     // Коралловий - втрата з'єднання
    );

    private final Label trayStatusLabel;
    private final Label statusLabel;
    private final Circle statusCircle;

    /**
     * Приватний конструктор для Builder pattern
     */
    private DeviceStatusUtil(Label statusLabel, Circle statusCircle, Label trayStatusLabel) {
        this.trayStatusLabel = trayStatusLabel;
        this.statusLabel = statusLabel;
        this.statusCircle = statusCircle;
    }

    /**
     * Статична фабрика для створення екземпляра з прив'язкою до UI елементів
     */
    public static DeviceStatusUtil bindTo(Label statusLabel, Circle statusCircle, Label trayStatusCircle ) {
        return new DeviceStatusUtil(statusLabel, statusCircle, trayStatusCircle);
    }



    /**
     * Підключає device listener для автоматичного оновлення статусу
     */
    public DeviceStatusUtil listenTo(Device device) {
        device.addStatusListener((oldStatus, newStatus) -> Platform.runLater(() -> setStatus(newStatus)
        ));
        return this;
    }

    /**
     * Встановлює статус програмно
     */
    public DeviceStatusUtil setStatus(DeviceStatus status) {
        statusLabel.setText(status.getDisplayText());
        if (statusCircle != null) {
            statusCircle.setFill(STATUS_COLORS.get(status));
        }
        if (trayStatusLabel != null) {
            ((Circle) trayStatusLabel.getGraphic()).setFill(STATUS_COLORS.get(status));
            trayStatusLabel.getTooltip().setText(status.getDisplayText());
        }
        return this;
    }

    /**
     * Зручні методи для конкретних статусів
     */
    public DeviceStatusUtil setStopped() {
        return setStatus(DeviceStatus.STOPPED);
    }

    public DeviceStatusUtil setStarting() {
        return setStatus(DeviceStatus.TRY_START);
    }

    public DeviceStatusUtil setWorking() {
        return setStatus(DeviceStatus.WORKING);
    }

    public DeviceStatusUtil setError() {
        return setStatus(DeviceStatus.ERROR);
    }
}

