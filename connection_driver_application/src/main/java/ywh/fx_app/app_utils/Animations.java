package ywh.fx_app.app_utils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.util.Duration;

public class Animations {
    private Animations() {
    }

    public static void shake(Node node, int duration, EventHandler<ActionEvent> actionEvent) {
        node.setTranslateX(0);
        TranslateTransition shake = new TranslateTransition(Duration.millis(duration), node);
        shake.setInterpolator(Interpolator.LINEAR); // Можно выбрать другой интерполятор для изменения характера анимации
        shake.setFromX(0);
        shake.setByX(9); // Амплитуда дрожания (можно изменить)
        shake.setCycleCount(6); // Количество циклов (смен направления)
        shake.setAutoReverse(true); // Автообратное воспроизведение, чтобы эффект возвращался в исходное положение
        shake.setOnFinished(e -> {
            node.setTranslateX(0);
            actionEvent.handle(e);
        });

        shake.play();
    }

    public static void shake(Node node, int duration) {
        shake(node, duration, ex -> {
        });
    }

    public static void fadeIn(Node node, int duration) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(duration), node);
        fadeIn.setFromValue(-50);
        fadeIn.setDelay(Duration.millis(70));
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> node.setOpacity(1));
        fadeIn.play();
    }

    public static void fadeIn(Node node, int duration, EventHandler<ActionEvent> actionEvent) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(duration), node);
        fadeIn.setFromValue(-50);
        fadeIn.setDelay(Duration.millis(70));
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            actionEvent.handle(e);
            node.setOpacity(1);
        });
        fadeIn.play();
    }

    public static void fadeOutFadeIn(Node fadeOutNode, Node fadeInNode, int duration, EventHandler<ActionEvent> actionEvent) {
        fadeOutNode.setOpacity(1);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(duration), fadeOutNode);
        fadeOut.setFromValue(50);  // Начальная прозрачность
        fadeOut.setDelay(Duration.millis(50));
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event ->
        {
            actionEvent.handle(event);
            fadeInNode.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(duration), fadeInNode);
            fadeIn.setFromValue(-50);
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }


    public static FadeTransition fadeOut(Node node, int duration) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(duration), node);
        fadeOut.setFromValue(50);  // Начальная прозрачность
        fadeOut.setDelay(Duration.millis(50));
        fadeOut.setToValue(0);
        fadeOut.play();
        return fadeOut;
    }

    public static FadeTransition fadeOut(Node node, int duration, EventHandler<ActionEvent> actionEvent) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(duration), node);
        fadeOut.setFromValue(50);  // Начальная прозрачность
        fadeOut.setDelay(Duration.millis(50));
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(actionEvent);// Конечная прозрачность
        fadeOut.play();
        return fadeOut;
    }

}
