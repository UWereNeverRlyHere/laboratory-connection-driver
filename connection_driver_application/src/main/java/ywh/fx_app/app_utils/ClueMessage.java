package ywh.fx_app.app_utils;

import com.jfoenix.controls.JFXTooltip;
import javafx.animation.FadeTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.util.Duration;

public class ClueMessage {
    private ClueMessage() {
    }
    private static Node currentNode;
    private static FadeTransition currentFade;
    private static JFXTooltip jfxTooltip;
    private static void refreshToolTip(String text, Node node) {
        if (node == currentNode && jfxTooltip!=null && jfxTooltip.isShowing()) return;

        if (jfxTooltip != null) {
            jfxTooltip.hide();
            jfxTooltip = null;
        }
        currentNode = node;
        jfxTooltip = new JFXTooltip();
        jfxTooltip.setText(text);
        jfxTooltip.setAutoHide(true);
        jfxTooltip.setHideOnEscape(true);
        jfxTooltip.setHideDelay(Duration.millis(350));
        jfxTooltip.getScene().getRoot().setVisible(false);
        // Создаем новую анимацию
        jfxTooltip.setOnShown(evt -> {
            if (currentFade != null) {
                currentFade.stop();
            }
            currentFade = new FadeTransition();
            Node tooltipNode = jfxTooltip.getScene().getRoot();
            Animations.fadeIn(tooltipNode,350);
            // Обновляем параметры анимации
            currentFade.setDuration(Duration.millis(1200));
            currentFade.setNode(tooltipNode);
            tooltipNode.setOpacity(1.0);
            currentFade.setFromValue(1.0);
            currentFade.setToValue(0);
            currentFade.setDelay(Duration.millis(7000));

            // Прерывание при скрытии через AutoHide
            currentFade.setOnFinished(e -> {
                if (currentFade != null) {
                    currentFade.stop();
                    currentFade = null;
                    currentNode = null;
                }
                jfxTooltip.hide();

            });
            currentFade.play();
            UiUtils.pauseAndDo(100,ev-> tooltipNode.setVisible(true));

        });
        jfxTooltip.setOnAutoHide(ev -> {
            if (currentFade != null) {
                currentFade.setDelay(Duration.ZERO);
                currentFade.playFromStart();
            }
        });

    }
    public static void hideClue() {
        if (jfxTooltip != null  && currentFade != null) {
            currentFade.setDelay(Duration.ZERO);
            currentFade.playFromStart();

        }
    }

    public static void showClueAboveCenter(String text, Node node) {
        refreshToolTip(text,node);
        jfxTooltip.setPos(Pos.TOP_CENTER);
        jfxTooltip.setMargin(0);
        jfxTooltip.show(node, 0, 0);
    }

    public static void showClueBottomRight(String text, Node node) {
        Bounds boundsInScreen = node.localToScreen(node.getBoundsInLocal());
        if (boundsInScreen == null) return;
        refreshToolTip(text,node);
        jfxTooltip.show(node.getScene().getWindow(), boundsInScreen.getMinX(), boundsInScreen.getMinY());
    }

}
