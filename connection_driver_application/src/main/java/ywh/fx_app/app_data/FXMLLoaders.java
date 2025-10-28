package ywh.fx_app.app_data;

import javafx.fxml.FXMLLoader;
import ywh.fx_app.application.DriverApp;

import java.util.Objects;

public enum FXMLLoaders {
    DEVICE("device-window.fxml"),
    DEVICE_PRINT_AND_FILE("device-print-file-data-window.fxml"),
    TRAY("tray-window.fxml"),
    CLARIFICATION_WINDOW("clarification/clarification-window.fxml"),
    CLARIFICATION_CONTAINER("clarification/clarification-container.fxml"),
    API_CONFIG("configs/api-config-window.fxml"),
    FTP_CONFIG("configs/ftp-config-window.fxml"),
    SERIAL_CONFIG("configs/serial-config-window.fxml"),
    ;
    private final String url;

    FXMLLoaders(String url) {
        this.url = url;
    }
    public FXMLLoader getLoader() {
        return new FXMLLoader(Objects.requireNonNull(DriverApp.class.getResource("/fxml/"+url)));
    }
}
