package ywh.fx_app.configs;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import ywh.commons.SerialPortUtil;
import ywh.commons.data.SerialPortMetaData;
import ywh.fx_app.app_binders.PropertyBinders;
import ywh.fx_app.app_binders.SettingsBind;
import ywh.services.data.serial_port.*;

public class SerialConfigWindowController {
    @FXML
    public TitledPane serialTitledPane;
    @FXML
    @SettingsBind(targetModel = SerialParams.class, targetField = "portName", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<String> portComboBox;
    @FXML
    @SettingsBind(targetModel = SerialParams.class, targetField = "baudRate", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<BaudRate> baudRateComboBox;
    @FXML
    @SettingsBind(targetModel = SerialParams.class, targetField = "dataBits", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<DataBits> dataBitsComboBox;
    @FXML
    @SettingsBind(targetModel = SerialParams.class, targetField = "stopBits", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<StopBits> stopBitsComboBox;
    @FXML
    @SettingsBind(targetModel = SerialParams.class, targetField = "parity", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<Parity> parityComboBox;

    @FXML
    private void initialize() {

        parityComboBox.getItems().addAll(Parity.values());
        baudRateComboBox.getItems().addAll(BaudRate.values());
        dataBitsComboBox.getItems().addAll(DataBits.values());
        stopBitsComboBox.getItems().addAll(StopBits.values());
    }

    public void selectPort(String portName){
        SerialPortUtil.getAllSystemSerialPorts(portName).thenAccept(ports -> {
            Platform.runLater(() -> {
                ports.forEach(port -> portComboBox.getItems().add(port.getName()));
                ports.stream().filter(SerialPortMetaData::isSelected).findFirst().ifPresent(port -> portComboBox.getSelectionModel().selectItem(port.getName()));
            });
        });
    }
}
