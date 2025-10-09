package ywh.fx_app.device;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import ywh.fx_app.app_binders.PropertyBinders;
import ywh.fx_app.app_binders.SettingsBind;
import ywh.fx_app.app_custom_nodes.ChangeableButton;
import ywh.fx_app.app_custom_nodes.FocusableTitledPane;
import ywh.fx_app.app_custom_nodes.PathChoiceField;
import ywh.services.data.enums.FileResultActions;
import ywh.services.printing.PrintersService;
import ywh.services.printing.PrintingMethod;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.settings.data.FileResultProcessorSettings;
import ywh.services.settings.data.PrintSettings;
import ywh.logging.MainLogger;

import java.util.List;

public class DevicePrintAndFilesController {
    @FXML
    public TitledPane root;
    @FXML
    public VBox rootVbox;
    @FXML
    public FocusableTitledPane printingMethodComboBoxTitledPane;
    @FXML
    public FocusableTitledPane printerTitledPane;
    @FXML
    @SettingsBind(targetModel = PrintSettings.class, targetField = "docxPrint", binder = PropertyBinders.CHANGEABLE_BUTTON)
    public MFXButton printFromFileButton;
    @FXML
    @SettingsBind(targetModel = PrintSettings.class, targetField = "silentPrint", binder = PropertyBinders.CHANGEABLE_BUTTON)
    public ChangeableButton silentPrintButton;
    @FXML
    @SettingsBind(targetModel = PrintSettings.class, targetField = "printingMethod", binder = PropertyBinders.ENUM_COMBO_BOX)
    public ComboBox<PrintingMethod> printingMethodComboBox;
    @FXML
    public MFXButton refreshPrintersButton;
    @FXML
    @SettingsBind(targetModel = PrintSettings.class, targetField = "printerName", binder = PropertyBinders.STRING_COMBO_BOX)
    public ComboBox<String> printerComboBox;

    @FXML
    @SettingsBind(targetModel = FileResultProcessorSettings.class, targetField = "templateFilePath", binder = PropertyBinders.TEXT_FIELD)
    public PathChoiceField templateFileField;

    @FXML
    @SettingsBind(targetModel = FileResultProcessorSettings.class, targetField = "outputPathString", binder = PropertyBinders.TEXT_FIELD)
    public PathChoiceField outputPathField;


    @Setter
    @Getter
    private DeviceManager deviceManager;
    @Setter
    @Getter
    private DeviceWindowController parentController;
    @Getter
    private DeviceSettings deviceSettings;
    public static final List<FileResultActions> REQUIRED_ACTIONS = List.of(FileResultActions.CREATE_DBF_FILE, FileResultActions.PRINT, FileResultActions.SAVE_DOCX, FileResultActions.SAVE_PDF);


    public void setDeviceSettings(DeviceSettings deviceSettings) {
        this.deviceSettings = deviceSettings;
        outputPathField.setDeviceSettings(deviceSettings);
    }

    @FXML
    public void initialize() {
        printingMethodComboBox.setItems(FXCollections.observableList(List.of(PrintingMethod.values())));
    }


    @FXML
    public void refreshPrinters() {
        printerComboBox.getItems().clear();
        List<String> availablePrinters = PrintersService.getAvailableActivePrintersJNA();
        printerComboBox.getItems().addAll(availablePrinters);
        // Після оновлення списку принтерів спробуємо встановити принтер за замовчуванням
        if (printerComboBox.getValue() == null || !availablePrinters.contains(printerComboBox.getValue())) {
            setPrinterFromSettings();
        }
    }

    protected void setPrinterFromSettings() {
        if (deviceSettings != null && deviceSettings.getPrintSettings().getPrinterName() != null && !deviceSettings.getPrintSettings().getPrinterName().isEmpty()) {
            printerComboBox.setValue(deviceSettings.getPrintSettings().getPrinterName());
        } else {
            // Якщо принтер не встановлений в налаштуваннях, спробуємо встановити за замовчуванням
            try {
                String defaultPrinter = PrintersService.getDefaultPrinterViaPowerShell();
                if (defaultPrinter != null && printerComboBox.getItems().contains(defaultPrinter)) {
                    printerComboBox.setValue(defaultPrinter);
                }
            } catch (Exception e) {
                MainLogger.error("Не вдалося встановити принтер за замовчуванням", e);
            }
        }
    }





    @Deprecated
    protected void initializeButtons() {

    }

    public void disablePrintAndFilesFieldsExceptOutput() {
        rootVbox.getChildren().removeIf(node -> {
            String id = node.getId();
            if (id != null && !id.toLowerCase().contains("output")) {
                node.setDisable(true);
                return true; // видалити
            } else if (node.getId() == null && node instanceof Parent parent) {
                parent.getChildrenUnmodifiable().forEach(child -> {
                    if (child.getId() != null && !child.getId().toLowerCase().contains("output")) {
                        child.setDisable(true);
                    }
                });
                return true; // видалити
            }
            return false; // не видаляти
        });


    }

    public Node getRoot() {
        return root;
    }
}
