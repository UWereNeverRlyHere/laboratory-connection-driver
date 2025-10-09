package ywh.fx_app.configs;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import ywh.fx_app.app_binders.PropertyBinders;
import ywh.fx_app.app_binders.SettingsBind;
import ywh.fx_app.app_utils.MfxComboBoxAdapter;
import ywh.services.data.enums.FileResultActions;
import ywh.services.settings.EncryptedSettings;
import ywh.services.settings.data.ApiSettings;

import java.util.Collections;
import java.util.List;

public class ApiConfigWindowController {
    @FXML
    public TitledPane apiTitledPane;
    @FXML
    @SettingsBind(targetModel = ApiSettings.class, targetField = "resultUrl", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXFilterComboBox<String> resultsUrlCombo;

    @FXML
    @SettingsBind(targetModel = ApiSettings.class, targetField = "orderUrl", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXFilterComboBox<String> ordersUrlCombo;

    @FXML
    @SettingsBind(targetModel = ApiSettings.class, targetField = "timeOut", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<Integer> timeOutComboBox;
    @FXML
    @SettingsBind(targetModel = ApiSettings.class, targetField = "orderTimeOut", binder = PropertyBinders.MFX_COMBO_BOX)
    public MFXComboBox<Integer> orderTimeOutComboBox;


    public static final List<FileResultActions> REQUIRED_ACTIONS = Collections.singletonList(FileResultActions.API);



    @FXML
    private void initialize() {
        timeOutComboBox.getItems().addAll(10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000);
        timeOutComboBox.getSelectionModel().selectItem(30000);
        orderTimeOutComboBox.getItems().addAll(1000, 2000, 3000, 4000, 5000, 7000, 6000, 8000, 9000, 10000, 15000, 20000);
        orderTimeOutComboBox.getSelectionModel().selectItem(7000);
        MfxComboBoxAdapter.makeComboBoxFullyClickable(timeOutComboBox);
        refreshUrls();
    }

    public void refreshUrls() {
        var apiSettingsList = EncryptedSettings.getApiSettingsList();
        resultsUrlCombo.getItems().clear();
        ordersUrlCombo.getItems().clear();
        apiSettingsList.forEach(apiSettings -> {
            if (!resultsUrlCombo.getItems().contains(apiSettings.getResultUrl())) {
                resultsUrlCombo.getItems().add(apiSettings.getResultUrl());
            }
            if (!ordersUrlCombo.getItems().contains(apiSettings.getOrderUrl())) {
                ordersUrlCombo.getItems().add(apiSettings.getOrderUrl());
            }
        });

    }
}
