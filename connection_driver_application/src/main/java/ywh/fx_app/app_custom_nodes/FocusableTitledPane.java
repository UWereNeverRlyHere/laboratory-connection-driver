package ywh.fx_app.app_custom_nodes;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FocusableTitledPane extends TitledPane {
    public final StringProperty title = new SimpleStringProperty();

    public final StringProperty titleProperty() {
        return title;
    }

    public final void setTitle(String value) {
        setText(value);
        title.set(value);
    }

    public final String getTitle() {
        return title.get();
    }

    private final Path leftOverlay = new Path();
    private final Path rightOverlay = new Path();
    private final Set<Node> processedNodes = new HashSet<>();

    // Додаємо прапорець: чи є фокус усередині цієї панелі
    private final BooleanProperty innerFocused = new SimpleBooleanProperty(false);

    // Слухач для focusOwner у Scene (зберігаємо, щоб коректно перевішувати при зміні сцени)
    private ChangeListener<Node> focusOwnerListener;

    @Getter
    private final AnchorPane innerAnchorPane;

    public FocusableTitledPane(String title, Node content) {
        this.setTitle(title);
        this.innerAnchorPane = new AnchorPane();
        initialize();
        if (content != null) {
            addToInnerAnchorPane(content);
        }
    }

    public FocusableTitledPane() {
        this.innerAnchorPane = new AnchorPane();
        initialize();
    }

    private void initialize() {
        this.setAnimated(false);
        this.setCollapsible(false);
        this.setContentDisplay(ContentDisplay.BOTTOM);
        this.setMaxWidth(Double.MAX_VALUE);
        this.getStyleClass().add("field-style");

        VBox.setVgrow(this, Priority.ALWAYS);
        HBox.setHgrow(this, Priority.ALWAYS);

        this.setFocusTraversable(false);

        innerAnchorPane.getStyleClass().add("no-padding-box");

        super.setContent(innerAnchorPane);

        leftOverlay.getStyleClass().add("focus-border");
        rightOverlay.getStyleClass().add("focus-border");

        leftOverlay.setMouseTransparent(true);
        rightOverlay.setMouseTransparent(true);

        layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (!getChildren().contains(leftOverlay)) getChildren().add(leftOverlay);
            if (!getChildren().contains(rightOverlay)) getChildren().add(rightOverlay);
            updateShape(newB.getWidth(), newB.getHeight());
        });

        contentProperty().addListener((obs, oldContent, newContent) -> {
            if (newContent != null && newContent != innerAnchorPane) {
                addToInnerAnchorPane(newContent);
                super.setContent(innerAnchorPane);
            }
        });

        innerAnchorPane.getChildren().addListener((javafx.collections.ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::setupFocusListeners);
                }
            }
        });

        // Глобальне відстеження зміни focusOwner у Scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && focusOwnerListener != null) {
                oldScene.focusOwnerProperty().removeListener(focusOwnerListener);
            }
            if (newScene != null) {
                focusOwnerListener = (ObservableValue<? extends Node> o, Node oldOwner, Node newOwner) -> {
                    boolean inside = isNodeInsideThis(newOwner);
                    updateInnerFocus(inside);
                };
                newScene.focusOwnerProperty().addListener(focusOwnerListener);

                // Ініціалізація стану при підключенні до сцени
                Node fo = newScene.getFocusOwner();
                updateInnerFocus(isNodeInsideThis(fo));
            }
        });
    }

    public void addToInnerAnchorPane(Node content) {
        if (content != null) {
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);

            innerAnchorPane.getChildren().clear();
            innerAnchorPane.getChildren().add(content);
            setupFocusListeners(content);
        }
    }

    public ObservableList<Node> getInnerChildren() {
        return innerAnchorPane.getChildren();
    }

    private void setupFocusListeners(Node node) {
        if (node == null || processedNodes.contains(node)) {
            return;
        }

        processedNodes.add(node);

        // Робимо елементи фокусованими для Tab-навігації, але
        // не запускаємо анімацію окремо для кожного фокусу.
        if (isTargetControl(node)) {
            node.setFocusTraversable(true);
            node.focusedProperty().addListener((obs, oldV, newV) -> {
                // Оновлюємо глобальний стан, спираючись на фактичного focusOwner
                updateInnerFocus(isAnyInnerNodeFocused());
            });
        } else if (node instanceof Button button) {
            button.focusedProperty().addListener((obs, oldV, newV) -> {
                updateInnerFocus(isAnyInnerNodeFocused());
            });
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                setupFocusListeners(child);
            }
        }
    }

    private boolean isTargetControl(Node node) {
        return node instanceof TextField
                || node instanceof ComboBox
                || node.getClass().getSimpleName().toLowerCase().contains("textfield")
                || node.getClass().getSimpleName().toLowerCase().contains("combobox");
    }

    // Залишаємо для сумісності, але переводимо на глобальний підхід
    public void addFocusListener(Node node) {
        if (isTargetControl(node) && !processedNodes.contains(node)) {
            processedNodes.add(node);
            node.focusedProperty().addListener((obs, oldV, newV) -> {
                updateInnerFocus(isAnyInnerNodeFocused());
            });
        }
    }

    protected void updateShape(double w, double h) {
        final double radius = 6.0;
        final double overlap = 1.0;

        if (w < 2 * radius || h < 2 * radius) {
            leftOverlay.getElements().clear();
            rightOverlay.getElements().clear();
            return;
        }

        double hw = w / 2;

        leftOverlay.getElements().setAll(
                new MoveTo(hw + overlap, h),
                new LineTo(radius, h),
                new ArcTo(radius, radius, 0, 0, h - radius, false, true),
                new LineTo(0, radius),
                new ArcTo(radius, radius, 0, radius, 0, false, true),
                new LineTo(hw + overlap, 0)
        );

        rightOverlay.getElements().setAll(
                new MoveTo(hw - overlap, h),
                new LineTo(w - radius, h),
                new ArcTo(radius, radius, 0, w, h - radius, false, false),
                new LineTo(w, radius),
                new ArcTo(radius, radius, 0, w - radius, 0, false, false),
                new LineTo(hw - overlap, 0)
        );

        double lineLength = hw - radius + overlap;
        double arcSegment = Math.PI * radius / 2.0;
        double lineSidePart = h - 2 * radius;
        double pathLength = lineLength + arcSegment + lineSidePart + arcSegment + lineLength;

        leftOverlay.getStrokeDashArray().setAll(pathLength, pathLength);
        leftOverlay.setStrokeDashOffset(pathLength);

        rightOverlay.getStrokeDashArray().setAll(pathLength, pathLength);
        rightOverlay.setStrokeDashOffset(pathLength);
    }

    private final AtomicBoolean isAnimated = new AtomicBoolean(false);

    // Запускаємо анімацію лише при реальній зміні стану
    protected void animateBorder(boolean show) {
        // Якщо стан не змінився — нічого не робимо (уникаємо перемальовування)
        if (isAnimated.get() == show) {
            return;
        }

        double pathLength = leftOverlay.getStrokeDashArray().isEmpty() ? 0 : leftOverlay.getStrokeDashArray().get(0);
        isAnimated.set(show);
        if (pathLength == 0) return;

        KeyValue startLeft = new KeyValue(leftOverlay.strokeDashOffsetProperty(), show ? pathLength : 0, Interpolator.EASE_OUT);
        KeyValue startRight = new KeyValue(rightOverlay.strokeDashOffsetProperty(), show ? pathLength : 0, Interpolator.EASE_OUT);

        KeyValue endLeft = new KeyValue(leftOverlay.strokeDashOffsetProperty(), show ? 0 : pathLength, Interpolator.EASE_BOTH);
        KeyValue endRight = new KeyValue(rightOverlay.strokeDashOffsetProperty(), show ? 0 : pathLength, Interpolator.EASE_BOTH);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, startLeft, startRight),
                new KeyFrame(Duration.seconds(0.25), endLeft, endRight)
        );
        tl.play();
    }

    // Допоміжні методи

    // Перевіряє, чи належить вузол цій панелі (включно з усіма нащадками)
    private boolean isNodeInsideThis(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur == this) return true;
            cur = cur.getParent();
        }
        return false;
    }

    // Чи якийсь вузол усередині панелі має фокус
    private boolean isAnyInnerNodeFocused() {
        Scene s = getScene();
        if (s == null) return false;
        return isNodeInsideThis(s.getFocusOwner());
    }

    // Оновлює глобальний стан та, за потреби, запускає/зупиняє анімацію
    private void updateInnerFocus(boolean inside) {
        if (innerFocused.get() != inside) {
            innerFocused.set(inside);
            animateBorder(inside);
        }
    }
}