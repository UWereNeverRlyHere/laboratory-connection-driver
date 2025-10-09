package ywh.fx_app.app_utils;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import ywh.commons.Task;

public class UiUtils {
    private UiUtils() {
    }

    public static void playCircleAnimation(Pane stackPane, Runnable task) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setOpacity(1.0);
        stackPane.getChildren().add(progress);
        progress.setMaxSize(50, 50); // или нужный размер
        StackPane.setAlignment(progress, Pos.CENTER);
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), progress);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        Task.startSequential(() -> {
            task.run();
            Platform.runLater(() -> {
                rotate.stop();
                stackPane.getChildren().remove(progress);
            });
        });
    }

    public static RotateTransition getCircleAnimation(Pane stackPane) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setOpacity(1.0);
        stackPane.getChildren().add(0,progress);
        progress.setMaxSize(50, 50); // или нужный размер
        StackPane.setAlignment(progress, Pos.CENTER);
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), progress);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        return rotate;
    }


    public static void pauseAndDo(double pauseTime, EventHandler<ActionEvent> actionEvent) {
        PauseTransition pause = new PauseTransition(Duration.millis(pauseTime));
        pause.setOnFinished(actionEvent);
        pause.play();
    }
}
