package ywh.fx_app.app_utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class StageUtils {

    private StageUtils() {
    }

    public static void setMinStageBoundsByScreenSize(Stage stage, double widthPercent, double heightPercent) {
        // Получение размеров экрана с учетом системного масштабирования
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Получение ширины и высоты экрана
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        // Установка ширины и высоты окна: 40% от ширины и высоты экрана
        double targetWidth = screenWidth * widthPercent;
        double targetHeight = screenHeight * heightPercent;
        stage.setMinWidth(targetWidth);
        stage.setMinHeight(targetHeight);
    }


    public static void setDefaultStageBounds(Stage stage, double widthPercent, double heightPercent) {
        // Получение размеров экрана с учетом системного масштабирования
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Получение ширины и высоты экрана
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        // Установка ширины и высоты окна: 40% от ширины и высоты экрана
        double targetWidth = screenWidth * widthPercent;
        double targetHeight = screenHeight * heightPercent;


        stage.setWidth(targetWidth);
        stage.setHeight(targetHeight);

        // Центрируем окно на экране
        stage.setX((screenBounds.getWidth() - targetWidth) / 2);
        stage.setY((screenBounds.getHeight() - targetHeight) / 2);
    }

    public static void setUpRoundCorners(Scene scene, Stage stage) {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(stage.widthProperty());
        clip.heightProperty().bind(stage.heightProperty());
        clip.setArcWidth(21);
        clip.setArcHeight(21);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getRoot().setClip(clip);
    }

}