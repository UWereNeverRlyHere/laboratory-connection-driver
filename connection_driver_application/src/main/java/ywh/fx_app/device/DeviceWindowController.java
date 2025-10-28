package ywh.fx_app.device;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.CheckComboBox;
import ywh.fx_app.app_binders.PropertyBinders;
import ywh.fx_app.app_binders.SettingsBind;
import ywh.fx_app.app_custom_nodes.ChangeableButton;
import ywh.fx_app.app_exceptions.SettingsValidationException;
import ywh.fx_app.app_managers.DisableStateManager;
import ywh.fx_app.app_utils.ClueMessage;
import ywh.fx_app.application.AppStaticConfig;
import ywh.fx_app.configs.ApiConfigWindowController;
import ywh.fx_app.configs.SerialConfigWindowController;
import ywh.services.data.enums.FileResultActions;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.services.settings.data.DeviceSettings;
import ywh.logging.MainLogger;

import java.util.List;

public class DeviceWindowController {
    @FXML
    public StackPane mainStackPane;
    @FXML
    public VBox centerVBox;
    @FXML
    @SettingsBind(targetModel = CommunicatorSettings.class, targetField = "host", binder = PropertyBinders.TEXT_FIELD)
    public MFXTextField hostField;
    @FXML
    @SettingsBind(targetModel = CommunicatorSettings.class, targetField = "port", binder = PropertyBinders.TEXT_FIELD)
    public MFXTextField portField;
    @FXML
    @SettingsBind(targetModel = DeviceSettings.class, targetField = "actions", binder = PropertyBinders.CHECK_COMBO_BOX)
    public CheckComboBox<FileResultActions> modeComboBox;
    /*  @FXML
      @SettingsBind(targetModel = DeviceSettings.class, targetField = "clarificationWindow", binder = PropertyBinders.CHECKBOX)
      public JFXCheckBox clarificationCB;
  */
    @FXML
    public MFXButton saveAndStartButton;
    @FXML
    public Circle statusCircle;
    @FXML
    public Label statusLabel;
    @FXML
    public TitledPane printAndFilesSettings;
    @FXML
    public DevicePrintAndFilesController printAndFilesSettingsController;
    @FXML
    public TitledPane apiConfig;
    @FXML
    public ApiConfigWindowController apiConfigController;
    @FXML
    public TitledPane serialConfig;
    public SerialConfigWindowController serialConfigController;


    @FXML
    @SettingsBind(targetModel = DeviceSettings.class, targetField = "serialNumber", binder = PropertyBinders.TEXT_FIELD)
    public MFXTextField serialNumberField;
    @FXML
    public ChangeableButton startStopButton;


    @Setter
    @Getter
    private DeviceManager deviceManager;

    private DisableStateManager buttonStateManager;


    @FXML
    public void initialize() {
        setupUI();
        printAndFilesSettings.setExpanded(true);
        addSetUpFieldValidators();
        setupButtonStateManager();
        startStopButton.addDefaultAction(() -> {
            if (startStopButton.isState()) {
                try {
                    deviceManager.restartDeviceAndSetStatus();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.WARNING, "Виникла помилка під час запуску: " + e.getMessage())
                            .showAndWait();
                }
            } else {
                deviceManager.stopDevice();
            }
        });
    }


    private void setupUI() {
        hostField.setText("127.0.0.1");
        portField.setText("5100");
        modeComboBox.getItems().addAll(AppStaticConfig.ALLOWED_ACTIONS);
    }


    private void setupButtonStateManager() {
        buttonStateManager = new DisableStateManager()
                .addCondition("invalidPort", false)
                .addCondition("invalidHost", false);
        buttonStateManager.bindTo(saveAndStartButton);
    }

    private void addSetUpFieldValidators() {
        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                portField.setText(oldValue);
                return;
            }
            int port = Integer.parseInt(newValue);
            boolean check = port < 1024 || port > 65535;
            if (check) {
                ClueMessage.showClueBottomRight("Порт має бути в межах 1024–65535", portField);
            } else ClueMessage.hideClue();

            buttonStateManager.setCondition("invalidPort", check);
        });


        modeComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener<FileResultActions>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    ObservableList<FileResultActions> checkedIndices = modeComboBox.getCheckModel().getCheckedItems();
                    boolean checkResult = checkedIndices.stream().anyMatch(
                            List.of(FileResultActions.PRINT,
                                    FileResultActions.SAVE_DOCX,
                                    FileResultActions.SAVE_PDF,
                                    FileResultActions.CREATE_DBF_FILE)::contains);
                    if (!checkResult) {
                        printAndFilesSettings.setExpanded(false);
                        printAndFilesSettings.setDisable(true);
                    } else {
                        printAndFilesSettings.setDisable(false);
                        printAndFilesSettings.setExpanded(true);
                    }
                }
            }
        });
    }


    public List<FileResultActions> getSelectedActions() {
        return modeComboBox.getCheckModel().getCheckedItems();
    }


    @FXML
    public void saveAndSart() {
        try {
            deviceManager.saveAndStart();
        } catch (SettingsValidationException e) {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            var alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            MainLogger.error("Виникла помилка під час запуску", e);
        }
    }

    public Node getRoot() {
        return mainStackPane;
    }
}