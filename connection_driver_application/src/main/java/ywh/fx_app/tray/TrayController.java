package ywh.fx_app.tray;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRippler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import ywh.fx_app.device.DeviceFactory;
import ywh.services.device.parsers.IParser;

public class TrayController {
    @FXML
    public VBox trayVBox;
    @FXML
    public JFXButton btnLog;
    @FXML
    public JFXButton btnOpen;
    @FXML
    public JFXButton btnSettings;
    @FXML
    public JFXButton btnExit;
    @FXML
    public JFXButton btnInfoTable;

    @FXML
    public Label statusLabel;
    @FXML
    public Circle statusCircle;


    private Stage primaryStage;
    private Runnable onCloseAction;


    @FXML
    public void openMainWindow() {
        //TODO add main window
   /*     if (primaryStage != null) {
            // Використовуємо Platform.runLater для безпеки
            Platform.runLater(() -> {
                if (!primaryStage.isShowing()) {
                    primaryStage.show();
                }
                if (primaryStage.isIconified()) {
                    primaryStage.setIconified(false);
                }
                primaryStage.toFront();
                primaryStage.requestFocus();
            });
        }*/
    }


    @FXML
    public void exit() {
        onCloseAction.run();
    }


    @FXML
    public void initialize() {
        setUpDefaultButtons();
    }

    private void addNodeOnTop(Node node) {
        if (trayVBox.getChildren().contains(node)) return;
        trayVBox.getChildren().addFirst(node);
    }

    private void setUpDefaultButtons() {
        trayVBox.getChildren().clear();
        //trayVBox.getChildren().add(btnOpen);
        trayVBox.getChildren().add(btnExit);
    }

    public void setStageAndCloseAction(Stage primaryStage, Runnable onCloseAction) {
        this.primaryStage = primaryStage;
        this.onCloseAction = onCloseAction;
    }

    @FXML
    public void openInfoTable(ActionEvent actionEvent) {
        //TODO пока что не нужно
    }

    @FXML
    public void openSettingsWindow() {
        //TODO пока что не нужно
    }

    public Label createNewStatusLbl(IParser parser, Circle statusCircle) {
        Label copy = new Label(parser.getName());
        copy.setTextFill(statusLabel.getTextFill());
        copy.setStyle(statusLabel.getStyle());

        copy.setPrefWidth(statusLabel.getPrefWidth());
        copy.setPrefHeight(statusLabel.getPrefHeight());

        copy.setMinWidth(statusLabel.getMinWidth());
        copy.setMinHeight(statusLabel.getMinHeight());

        copy.setMaxWidth(statusLabel.getMaxWidth());
        copy.setMaxHeight(statusLabel.getMaxHeight());

        copy.setWrapText(statusLabel.isWrapText());
        copy.setLayoutX(statusLabel.getLayoutX());
        copy.setLayoutY(statusLabel.getLayoutY());
        copy.setAlignment(statusLabel.getAlignment());
        Circle statusCircleCopy = new Circle(statusCircle.getRadius());
        statusCircleCopy.setFill(statusCircle.getFill());
        copy.setGraphic(statusCircleCopy);
        copy.setPadding(statusLabel.getPadding());
        copy.setTooltip(new Tooltip());
        copy.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY)
                DeviceFactory.openDeviceWindow(parser);
            else if (mouseEvent.getButton() == MouseButton.SECONDARY){
                //TODO OPEN LOG
                //https://github.com/FXMisc/RichTextFX
                // или "com.dlsc:FX-Console:1.0.5"
            }
        });
        copy.setPickOnBounds(true);
        var rippler = new JFXRippler(copy);
        rippler.setMaskType(JFXRippler.RipplerMask.FIT);
        rippler.setRipplerFill(Color.ALICEBLUE);
        rippler.setPosition(JFXRippler.RipplerPos.FRONT);
        addNodeOnTop(rippler);

        return copy;
    }

    public JFXButton copyJFXButton() {

        JFXButton copy = new JFXButton();
        copy.setText("");
        copy.setStyle(btnLog.getStyle());
        copy.getStyleClass().addAll(btnLog.getStyleClass());
        copy.setPrefWidth(btnLog.getPrefWidth());
        copy.setPrefHeight(btnLog.getPrefHeight());
        copy.setLayoutX(btnLog.getLayoutX());
        copy.setLayoutY(btnLog.getLayoutY());
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setAlignment(btnLog.getAlignment());
        copy.setContentDisplay(btnLog.getContentDisplay());

        return copy;
    }


}
