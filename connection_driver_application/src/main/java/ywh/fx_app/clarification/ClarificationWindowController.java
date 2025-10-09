package ywh.fx_app.clarification;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import ywh.commons.DateTime;
import ywh.fx_app.app_utils.MfxComboBoxAdapter;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.data.models.observation.ObservationData;

public class ClarificationWindowController {

    @FXML
    public TitledPane mainTitledPane;
    @FXML
    public VBox mainVbox;
    @FXML
    public MFXTextField ownerTF;
    @FXML
    public MFXTextField animalNameTF;
    @FXML
    public MFXTextField animalAgeTF;
    @FXML
    public MFXComboBox<AnimalType> animalCB;
    @FXML
    public MFXButton confirmBtn;
    @FXML
    public MFXButton cancelBtn;


    private ClarificationContainerController parentController;
    private ObservationData data;

    @FXML
    private void initialize() {
        animalCB.getItems().addAll(AnimalType.values());
        animalCB.getSelectionModel().selectFirst();
        setUpKeyListenersForField(ownerTF);
        setUpKeyListenersForField(animalNameTF);
        setUpKeyListenersForField(animalAgeTF);
        MfxComboBoxAdapter.makeComboBoxFullyClickable(animalCB);
    }

    protected void setUp(ClarificationContainerController parentController, ObservationData data) {
        this.parentController = parentController;
        this.data = data;
        populateFields();
        Platform.runLater(() -> ownerTF.requestFocus());
    }


    private void setUpKeyListenersForField(Node field) {
        field.addEventFilter(KeyEvent.KEY_RELEASED, evt -> {
            KeyCode code = evt.getCode();
            if (evt.isAltDown()) {
                switch (code) {
                    case ENTER -> confirm();
                    case ESCAPE -> cancel();
                    case UP -> animalCB.getSelectionModel().selectPrevious();
                    case DOWN -> animalCB.getSelectionModel().selectNext();
                }
                evt.consume();
            } else if (code == KeyCode.ENTER) {
                handleEnterNavigation(field);
                evt.consume();

            }
        });
    }

    private void handleEnterNavigation(Node source) {
        Platform.runLater(() -> {
            if (source == ownerTF) {
                animalNameTF.requestFocus();
            } else if (source == animalNameTF) {
                animalAgeTF.requestFocus();
            } else if (source == animalAgeTF) {
                ownerTF.requestFocus();
            }
        });
    }


    private void populateFields() {
        mainTitledPane.setText(data.getPrintDate().orElse(DateTime.getDateTime()) + " - [" + data.getId().orElse("0") + "] | " + data.getAnalyzerName().orElse(""));
        data.getOwner().ifPresent(ownerTF::setText);
        data.getAnimalName().ifPresent(animalNameTF::setText);
        data.getAge().ifPresent(animalAgeTF::setText);
        if (data.getAnimalType() != null) {
            animalCB.getSelectionModel().selectIndex(animalCB.getItems().indexOf(data.getAnimalType()));
        }
    }

    @FXML
    public void confirm() {

        data.putOwner(ownerTF.getText());
        data.putAnimalName(animalNameTF.getText());
        data.putAge(animalAgeTF.getText());
        if (animalCB.getValue() != null) {
            data.putAnimalType(animalCB.getValue().getUaDefaultName());
        }
        parentController.confirmSingle(this, data);

    }

    @FXML
    public void cancel() {
        parentController.confirmSingle(this, new ObservationData());
    }

    public Node getRoot() {
        return mainTitledPane;
    }
}