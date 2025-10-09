package ywh.fx_app.app_utils;

import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Comparator;

/**
 * @param <T> Комбо бокс с возможностью поиска
 *            Пример использования :  new AutoCompleteComboBoxListener<>(analyzerCodesCombo);
 */
public class AutoCompleteComboBoxListener<T> implements EventHandler<KeyEvent> {
    private final JFXComboBox<T> comboBox;
    private final ObservableList<T> data;
    private boolean moveCaretToPos = false;
    private boolean searhByContains = false;
    private int caretPos;

    public AutoCompleteComboBoxListener(final JFXComboBox<T> comboBox, boolean searhByContains) {
        this.comboBox = comboBox;
        data = comboBox.getItems();
        this.searhByContains = searhByContains;
        this.comboBox.setEditable(true);
        this.comboBox.setOnKeyPressed(t -> comboBox.hide());
        this.comboBox.setOnKeyReleased(AutoCompleteComboBoxListener.this);
    }

    @Override
    public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.UP) {
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if (event.getCode() == KeyCode.DOWN) {
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
            caretPos = -1;
            moveCaret(comboBox.getEditor().getText().length());
            return;
        } else if (event.getCode() == KeyCode.BACK_SPACE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        } else if (event.getCode() == KeyCode.DELETE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        }

        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT
                || event.isControlDown() || event.getCode() == KeyCode.HOME
                || event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER) {
            return;
        }

        try {
            ObservableList<T> list = FXCollections.observableArrayList();
            if (!searhByContains) {
                data.stream().filter(datum -> datum.toString().toLowerCase().startsWith(
                        AutoCompleteComboBoxListener.this.comboBox
                                .getEditor().getText().toLowerCase())).forEachOrdered(list::add);
            } else {
                String lowerCase = AutoCompleteComboBoxListener.this.comboBox.getEditor().getText().toLowerCase();
                if (lowerCase.isEmpty()) {
                    data.sort(Comparator.comparing(datum -> datum.toString().toLowerCase()));
                } else {
                    Comparator<T> parserComparator = Comparator.comparing(x -> x.toString().toLowerCase().contains(lowerCase));
                    data.sort(parserComparator.reversed());
                }
                list.addAll(data);
            }

            String t = comboBox.getEditor().getText();
            comboBox.setItems(list);
            comboBox.getEditor().setText(t);

            if (!moveCaretToPos)
                caretPos = -1;
            moveCaret(t.length());

            // Виправлення для нових версій JavaFX
            try {
                if (comboBox.getSkin() instanceof ComboBoxListViewSkin) {
                    ComboBoxListViewSkin<T> skin = (ComboBoxListViewSkin<T>) comboBox.getSkin();
                    ListView<T> listView = (ListView<T>) skin.getPopupContent();
                    listView.scrollTo(0);
                }
            } catch (Exception e) {
                // Якщо не вдається отримати доступ до ListView, просто ігноруємо
            }

            if (!list.isEmpty()) {
                comboBox.show();
            }
        } catch (Exception ignored) {
        }
    }

    private void moveCaret(int textLength) {
        if (caretPos == -1) {
            comboBox.getEditor().positionCaret(textLength);
        } else {
            comboBox.getEditor().positionCaret(caretPos);
        }
        moveCaretToPos = false;
    }
}