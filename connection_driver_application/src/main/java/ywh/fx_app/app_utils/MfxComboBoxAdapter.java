package ywh.fx_app.app_utils;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MfxComboBoxAdapter {

    /**
     * Для каждого MFXComboBox хранится свой флаг «первого показа»,
     * чтобы offset и первый hide-показ применялись только один раз.
     */
    private static final Map<MFXComboBox<?>, AtomicBoolean> firstShowMap = new WeakHashMap<>();



    /**
     * Делает MFXComboBox «полностью кликабельным»:
     * - клик/ENTER/SPACE/TAB — открывает или закрывает popup,
     * - стрелки UP/DOWN — меняют элемент и сразу скрывают popup.
     */
    public static <T> void makeComboBoxFullyClickable(MFXComboBox<T> comboBox) {
        comboBox.addEventFilter(MouseEvent.MOUSE_PRESSED, evt -> {
            togglePopup(comboBox);
            evt.consume();
        });

        comboBox.addEventFilter(KeyEvent.KEY_RELEASED, evt -> {
            KeyCode code = evt.getCode();
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                togglePopup(comboBox);
                evt.consume();
            } else if (code == KeyCode.UP) {
                comboBox.getSelectionModel().selectPrevious();
                show(comboBox);
                evt.consume();
            } else if (code == KeyCode.DOWN) {
                comboBox.getSelectionModel().selectNext();
                show(comboBox);
                evt.consume();
            }
        });
    }


    /**
     * Переключает состояние popup (открыто/закрыто).
     */
    private static <T> void togglePopup(MFXComboBox<T> comboBox) {
        if (comboBox.isShowing()) {
            hideBySelectionHack(comboBox);
        } else {
            show(comboBox);
        }
    }

    /**
     * Показывает popup. При первом вызове:
     * - устанавливает небольшой offset,
     * - делает show + скрытие для «инициализации» скина.
     */
    private static <T> void show(MFXComboBox<T> comboBox) {
        AtomicBoolean firstShow = firstShowMap
                .computeIfAbsent(comboBox, cb -> new AtomicBoolean(true));

        if (firstShow.getAndSet(false)) {
            comboBox.setPopupOffsetY(5);
            comboBox.show();
            hideBySelectionHack(comboBox);
        }

        if (!comboBox.isShowing()) {
            comboBox.show();
            // через runLater убеждаемся, что фокус точно поставится после показа
            Platform.runLater(comboBox::requestFocus);
        }
    }

    /**
     * Надёжный «хак» скрытия popup, использующий внутренний listener:
     * смена элемента в SelectionModel закрывает popup в MFXComboBoxSkin.
     */
    private static <T> void hideBySelectionHack(MFXComboBox<T> comboBox) {
        int current = comboBox.getSelectionModel().getSelectedIndex();
        comboBox.getSelectionModel().selectNext();
        comboBox.getSelectionModel().selectIndex(current);
    }
}