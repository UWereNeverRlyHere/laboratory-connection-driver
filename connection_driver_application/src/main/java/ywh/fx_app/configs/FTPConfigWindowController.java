package ywh.fx_app.configs;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TitledPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import ywh.fx_app.app_binders.PropertyBinders;
import ywh.fx_app.app_binders.SettingsBind;
import ywh.fx_app.app_data.FXMLLoaders;
import ywh.fx_app.app_exceptions.SettingsValidationException;
import ywh.fx_app.app_utils.StageUtils;
import ywh.services.settings.data.FtpSettings;

import java.io.IOException;


public class FTPConfigWindowController {
    @FXML
    @SettingsBind(targetModel = FtpSettings.class, targetField = "server", binder = PropertyBinders.TEXT_FIELD)
    public MFXTextField serverTF;
    @FXML
    @SettingsBind(targetModel = FtpSettings.class, targetField = "user", binder = PropertyBinders.TEXT_FIELD)
    public MFXTextField userTf;
    @FXML
    @SettingsBind(targetModel = FtpSettings.class, targetField = "password", binder = PropertyBinders.TEXT_FIELD)
    public MFXPasswordField passwordTF;
    @FXML
    public TitledPane ftpTitledPane;
    @FXML
    public MFXButton confirmBtn;
    @FXML
    public MFXButton cancelBtn;
    @Getter
    @Setter
    private Stage stage;
    @Setter
    private Runnable onConfirmAction;

    @FXML
    private void initialize() {
        confirmBtn.setDefaultButton(true);
        cancelBtn.setCancelButton(true);
        confirmBtn.disableProperty()
                .bind(serverTF.textProperty().isEmpty()
                        .or(userTf.textProperty().isEmpty())
                        .or(passwordTF.textProperty().isEmpty()));
    }

    public void showAndWait() {
        stage.showAndWait();
    }

    public static FTPConfigWindowController createAsModal(Stage parent) throws IOException, SettingsValidationException {
        FXMLLoader loader = FXMLLoaders.FTP_CONFIG.getLoader();
        Scene scene = new Scene(loader.load());
        FTPConfigWindowController controller = loader.getController();
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Налаштування FTP для результатів");
        stage.initStyle(StageStyle.UNDECORATED);
       // stage.getIcons().add(ImageLoader.APP_IMAGE.getFxImage());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        StageUtils.setMinStageBoundsByScreenSize(stage, 0.20, 0.28);
        controller.setStage(stage);
        return controller;
    }


    public void confirm(ActionEvent actionEvent)  {
      if (onConfirmAction != null)
          onConfirmAction.run();
        stage.close();
    }

    public void cancel(ActionEvent actionEvent) {
        stage.close();
    }
}
