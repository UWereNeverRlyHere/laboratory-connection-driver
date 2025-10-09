package ywh.fx_app.app_custom_nodes;

import com.jfoenix.controls.JFXTextField;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import ywh.fx_app.app_binders.SettingsBinder;
import ywh.fx_app.app_exceptions.SettingsValidationException;
import ywh.fx_app.application.DriverApp;
import ywh.fx_app.configs.FTPConfigWindowController;
import ywh.services.exceptions.DeviceValidationException;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.web.FtpClient;
import ywh.logging.MainLogger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class PathChoiceField extends FocusableTitledPane implements ITextField {


    public enum ChooserType {
        FILE,
        DIRECTORY,
        ;
    }

    @FXML
    @Getter
    private HBox hBox;
    @FXML
    @Getter
    private MFXButton button;
    @FXML
    @Getter
    private MFXButton ftpButton;
    @FXML
    @Setter
    private JFXTextField textField;

    @Setter
    @Getter
    private ChooserType chooserType = ChooserType.FILE;
    @Getter
    private boolean addFtpButton = false;

    @Override
    public String getFieldText() {
        return textField.getText();
    }

    @Override
    public void setFieldText(String text) {
        textField.setText(text);
    }

    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);

    public final BooleanProperty editableProperty() {
        return editable;
    }

    public StringProperty innerTextProperty() {
        return textField.textProperty();
    }

    public StringProperty promptTextProperty() {
        return textField.promptTextProperty();
    }

    public final boolean isEditable() {
        return editable.get();
    }

    public final void setEditable(boolean value) {
        editable.set(value);
    }

    public final String getPromptText() {
        return textField.promptTextProperty().get();
    }

    public void setPromptText(String value) {
        textField.setPromptText(value);
    }

    public PathChoiceField(String title, Node content) {
        super(title, content);
        initInternal();
    }

    public PathChoiceField() {
        super();
        initInternal();
    }

    private void initInternal() {
        FXMLLoader fxml = new FXMLLoader(
                DriverApp.class.getResource("/fxml/custom/path-choice-field.fxml"));
        fxml.setRoot(this);
        fxml.setController(this);
        try {
            fxml.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textField.setText("");
        textField.setFocusTraversable(this.isFocusTraversable());
        textField.editableProperty().bind(this.editableProperty());
        textField.focusTraversableProperty().bind(this.editableProperty());
        setupEventHandlers();
    }

    public void setAddFtpButton(boolean addFtpButton) {
        this.addFtpButton = addFtpButton;
        if (ftpButton != null) {
            if (!addFtpButton) {
                hBox.getChildren().remove(ftpButton);
                ftpButton = null;
            } else {
                if (!hBox.getChildren().contains(ftpButton)) {
                    int insertIndex = hBox.getChildren().indexOf(textField);
                    if (insertIndex > 0) {
                        hBox.getChildren().add(insertIndex, ftpButton);
                    }
                }
                ftpButton.setOnAction(e -> {
                    try {
                        openFtpSettings();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }

    private void setupEventHandlers() {
        button.setOnAction(this::openDialog);
        button.setOnMouseClicked(this::openOutputDirectory);
    }

    @Getter
    @Setter
    private DeviceSettings deviceSettings;

    @FXML
    private void openFtpSettings() {
        try {
            Stage parentStage = (Stage) this.getScene().getWindow();
            FTPConfigWindowController ftpController = FTPConfigWindowController.createAsModal(parentStage);

            SettingsBinder.fillAll(ftpController, deviceSettings);
            ftpController.setOnConfirmAction(() -> {
                try {
                    SettingsBinder.commitAll(ftpController, deviceSettings);
                    deviceSettings.getFileResultProcessorSettings().setUseFtp(true);
                    deviceSettings.getFileResultProcessorSettings().setOutputPath(Path.of(""));
                    textField.setText(deviceSettings.getFileResultProcessorSettings().getFtpSettings().getUiString());
                    FtpClient.checkConnection(deviceSettings.getFileResultProcessorSettings().getFtpSettings());
                } catch (SettingsValidationException e) {
                    MainLogger.error("Error setting up event handlers", e);
                } catch (DeviceValidationException e) {
                    new Alert(Alert.AlertType.INFORMATION, e.getMessage()).showAndWait();
                }
            });
            ftpController.showAndWait();
        } catch (SettingsValidationException | IOException e) {
            MainLogger.error("Error loading FTP settings ", e);
        }

    }


    private void openOutputDirectory(MouseEvent mouseEvent) {
        try {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {

                File file = new File(textField.getText());
                if (!file.exists() || textField.getText().contains(":")) {
                    String jarPath = PathChoiceField.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .getPath();
                    file = new File(jarPath).getParentFile();
                }
                Desktop.getDesktop().open(file);
            } else if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                openDialog(mouseEvent);
            }
        } catch (Exception e) {
            MainLogger.error("Error setting up event handlers", e);
        }
    }

    private void openDialog(Object event) {
        if (deviceSettings!= null && addFtpButton){
            deviceSettings.getFileResultProcessorSettings().setUseFtp(false);
        }
        if (chooserType == ChooserType.FILE) {
            var fileChooser = new FileChooser();
            File file = new File(textField.getText());
            File parentDir = file
                    .getAbsoluteFile()
                    .getParentFile();
            fileChooser.setInitialDirectory(parentDir);
            fileChooser.setInitialFileName(file.getName());
            var selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                textField.setText(selectedFile.getAbsolutePath());
            }
        } else {
            var directoryChooser = new DirectoryChooser();
            File selectedFile;
            try {
                directoryChooser.setInitialDirectory(new File(textField.getText()));
                selectedFile = directoryChooser.showDialog(null);
            } catch (Exception e) {
                directoryChooser.setInitialDirectory(new File("").getParentFile());
                selectedFile = directoryChooser.showDialog(null);
            }
            if (selectedFile != null) {
                textField.setText(selectedFile.getAbsolutePath());
            }
        }
    }

}