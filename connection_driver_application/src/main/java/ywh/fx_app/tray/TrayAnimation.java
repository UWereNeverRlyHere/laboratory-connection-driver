package ywh.fx_app.tray;

import javafx.animation.*;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class TrayAnimation {
    private TrayAnimation() {}

    // Тривалості анімацій в стилі Windows 11/macOS
    private static final int SHOW_DURATION_MILLIS = 230;
    private static final int HIDE_DURATION_MILLIS = 210;
    private static final int SHADOW_DURATION_MILLIS = 180;
    private static final int BLUR_DURATION_MILLIS = 180;
    private static final int SLIDE_DURATION_MILLIS = 220;

    // Кастомні easing функції в стилі Windows 11/macOS
    private static final Interpolator FLUENT_EASE_OUT = Interpolator.SPLINE(0.1, 0.7, 0.1, 1.0);
    private static final Interpolator FLUENT_EASE_IN = Interpolator.SPLINE(0.7, 0.0, 0.9, 0.3);
    private static final Interpolator FLUENT_EASE_IN_OUT = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
    private static final Interpolator BOUNCE_EASE = Interpolator.SPLINE(0.68, 0.0, 0.32, 1.0);

    /**
     * Анімація приховування в стилі Windows 11 - slide down + scale + fade + blur
     */
    public static void minimize(Stage stage) {
        Node root = stage.getScene().getRoot();
        
        // Створюємо комбіновану анімацію
        ParallelTransition hideAnimation = new ParallelTransition();
        
        // 1. Scale анімація (зменшення з нижнього краю)
        Scale scale = getOrCreateBottomScale(root);
        Timeline scaleTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(scale.xProperty(), 1.0, FLUENT_EASE_IN),
                new KeyValue(scale.yProperty(), 1.0, FLUENT_EASE_IN)
            ),
            new KeyFrame(Duration.millis(HIDE_DURATION_MILLIS), 
                new KeyValue(scale.xProperty(), 0.85, FLUENT_EASE_IN),
                new KeyValue(scale.yProperty(), 0.7, FLUENT_EASE_IN)
            )
        );
        
        // 2. Slide down анімація (зсув вниз)
        Translate translate = getOrCreateTranslate(root);
        Timeline slideTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(translate.yProperty(), 0.0, FLUENT_EASE_IN)),
            new KeyFrame(Duration.millis(SLIDE_DURATION_MILLIS), new KeyValue(translate.yProperty(), 30.0, FLUENT_EASE_IN))
        );
        
        // 3. Opacity анімація (fade out)
        Timeline opacityTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), 1.0, FLUENT_EASE_IN)),
            new KeyFrame(Duration.millis(HIDE_DURATION_MILLIS), new KeyValue(root.opacityProperty(), 0.0, FLUENT_EASE_IN))
        );
        
        // 4. Blur ефект (розмиття при зникненні)
        EffectContainer effects = getOrCreateEffects(root);
        Timeline blurTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(effects.blur.radiusProperty(), 0.0, FLUENT_EASE_IN)),
            new KeyFrame(Duration.millis(BLUR_DURATION_MILLIS), new KeyValue(effects.blur.radiusProperty(), 8.0, FLUENT_EASE_IN))
        );
        
        // 5. Shadow анімація (зменшення тіні)
        Timeline shadowTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(effects.shadow.radiusProperty(), 25.0, FLUENT_EASE_IN),
                new KeyValue(effects.shadow.offsetYProperty(), 8.0, FLUENT_EASE_IN)
            ),
            new KeyFrame(Duration.millis(SHADOW_DURATION_MILLIS), 
                new KeyValue(effects.shadow.radiusProperty(), 5.0, FLUENT_EASE_IN),
                new KeyValue(effects.shadow.offsetYProperty(), 2.0, FLUENT_EASE_IN)
            )
        );
        
        hideAnimation.getChildren().addAll(scaleTimeline, slideTimeline, opacityTimeline, blurTimeline, shadowTimeline);
        
        hideAnimation.setOnFinished(event -> {
            stage.hide();
            // Скидаємо значення для наступного показу
            resetProperties(root, scale, translate, effects);
        });
        
        hideAnimation.play();
    }

    /**
     * Анімація появи знизу вгору в стилі tray popup - slide up + elastic scale + fade + glass blur
     */
    public static void expand(@NotNull Stage stage) {
        Node root = stage.getScene().getRoot();
        
        // Підготовка до анімації
        setupInitialStateFromBottom(root, stage);
        
        // Комбінована анімація появи
        ParallelTransition showAnimation = new ParallelTransition();
        
        Scale scale = getOrCreateBottomScale(root);
        Translate translate = getOrCreateTranslate(root);
        EffectContainer effects = getOrCreateEffects(root);
        
        // 1. Slide up анімація (зсув знизу вгору)
        Timeline slideTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(translate.yProperty(), 40.0, FLUENT_EASE_OUT)),
            new KeyFrame(Duration.millis(SLIDE_DURATION_MILLIS * 0.6), new KeyValue(translate.yProperty(), -5.0, BOUNCE_EASE)),
            new KeyFrame(Duration.millis(SLIDE_DURATION_MILLIS), new KeyValue(translate.yProperty(), 0.0, FLUENT_EASE_OUT))
        );
        
        // 2. Elastic scale анімація з нижнього краю
        Timeline scaleTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(scale.xProperty(), 0.7, FLUENT_EASE_OUT),
                new KeyValue(scale.yProperty(), 0.5, FLUENT_EASE_OUT)
            ),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS * 0.7), 
                new KeyValue(scale.xProperty(), 1.02, BOUNCE_EASE),
                new KeyValue(scale.yProperty(), 1.05, BOUNCE_EASE)
            ),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS), 
                new KeyValue(scale.xProperty(), 1.0, FLUENT_EASE_OUT),
                new KeyValue(scale.yProperty(), 1.0, FLUENT_EASE_OUT)
            )
        );
        
        // 3. Smooth fade in
        Timeline opacityTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(root.opacityProperty(), 0.0, FLUENT_EASE_OUT)),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS * 0.3), new KeyValue(root.opacityProperty(), 0.8, FLUENT_EASE_OUT)),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS), new KeyValue(root.opacityProperty(), 1.0, FLUENT_EASE_OUT))
        );
        
        // 4. Glass blur ефект (з'ясування від розмитого до чіткого)
        Timeline blurTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(effects.blur.radiusProperty(), 12.0, FLUENT_EASE_OUT)),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS * 0.4), new KeyValue(effects.blur.radiusProperty(), 3.0, FLUENT_EASE_OUT)),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS), new KeyValue(effects.blur.radiusProperty(), 0.0, FLUENT_EASE_OUT))
        );
        
        // 5. Динамічна тінь (зростає знизу)
        Timeline shadowTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(effects.shadow.radiusProperty(), 0.0, FLUENT_EASE_OUT),
                new KeyValue(effects.shadow.offsetYProperty(), 0.0, FLUENT_EASE_OUT)
            ),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS * 0.6), 
                new KeyValue(effects.shadow.radiusProperty(), 30.0, FLUENT_EASE_OUT),
                new KeyValue(effects.shadow.offsetYProperty(), 10.0, FLUENT_EASE_OUT)
            ),
            new KeyFrame(Duration.millis(SHOW_DURATION_MILLIS), 
                new KeyValue(effects.shadow.radiusProperty(), 25.0, FLUENT_EASE_OUT),
                new KeyValue(effects.shadow.offsetYProperty(), 8.0, FLUENT_EASE_OUT)
            )
        );
        
        showAnimation.getChildren().addAll(slideTimeline, scaleTimeline, opacityTimeline, blurTimeline, shadowTimeline);
        showAnimation.play();
    }

    /**
     * Швидка анімація для hover ефекту з легким lift up
     */
    public static void createHoverEffect(Node node) {
        Scale scale = getOrCreateBottomScale(node);
        Translate translate = getOrCreateTranslate(node);
        EffectContainer effects = getOrCreateEffects(node);
        
        node.setOnMouseEntered(e -> {
            ParallelTransition hoverIn = new ParallelTransition();
            
            // Scale effect
            Timeline scaleIn = new Timeline(
                new KeyFrame(Duration.millis(100), 
                    new KeyValue(scale.xProperty(), 1.05, FLUENT_EASE_OUT),
                    new KeyValue(scale.yProperty(), 1.05, FLUENT_EASE_OUT)
                )
            );
            
            // Lift up effect
            Timeline liftIn = new Timeline(
                new KeyFrame(Duration.millis(120), 
                    new KeyValue(translate.yProperty(), -2.0, FLUENT_EASE_OUT)
                )
            );
            
            // Subtle blur effect
            Timeline blurIn = new Timeline(
                new KeyFrame(Duration.millis(80), 
                    new KeyValue(effects.blur.radiusProperty(), 1.0, FLUENT_EASE_OUT)
                )
            );
            
            // Enhanced shadow
            Timeline shadowIn = new Timeline(
                new KeyFrame(Duration.millis(100), 
                    new KeyValue(effects.shadow.radiusProperty(), 35.0, FLUENT_EASE_OUT),
                    new KeyValue(effects.shadow.offsetYProperty(), 12.0, FLUENT_EASE_OUT)
                )
            );
            
            hoverIn.getChildren().addAll(scaleIn, liftIn, blurIn, shadowIn);
            hoverIn.play();
        });
        
        node.setOnMouseExited(e -> {
            ParallelTransition hoverOut = new ParallelTransition();
            
            // Scale back
            Timeline scaleOut = new Timeline(
                new KeyFrame(Duration.millis(150), 
                    new KeyValue(scale.xProperty(), 1.0, FLUENT_EASE_IN_OUT),
                    new KeyValue(scale.yProperty(), 1.0, FLUENT_EASE_IN_OUT)
                )
            );
            
            // Drop down
            Timeline dropOut = new Timeline(
                new KeyFrame(Duration.millis(120), 
                    new KeyValue(translate.yProperty(), 0.0, FLUENT_EASE_IN_OUT)
                )
            );
            
            // Remove blur
            Timeline blurOut = new Timeline(
                new KeyFrame(Duration.millis(120), 
                    new KeyValue(effects.blur.radiusProperty(), 0.0, FLUENT_EASE_IN_OUT)
                )
            );
            
            // Reset shadow
            Timeline shadowOut = new Timeline(
                new KeyFrame(Duration.millis(150), 
                    new KeyValue(effects.shadow.radiusProperty(), 25.0, FLUENT_EASE_IN_OUT),
                    new KeyValue(effects.shadow.offsetYProperty(), 8.0, FLUENT_EASE_IN_OUT)
                )
            );
            
            hoverOut.getChildren().addAll(scaleOut, dropOut, blurOut, shadowOut);
            hoverOut.play();
        });
    }

    // Допоміжні класи та методи
    private static class EffectContainer {
        final DropShadow shadow;
        final GaussianBlur blur;
        final Blend blend;
        
        EffectContainer(DropShadow shadow, GaussianBlur blur, Blend blend) {
            this.shadow = shadow;
            this.blur = blur;
            this.blend = blend;
        }
    }

    private static Scale getOrCreateBottomScale(Node node) {
        return node.getTransforms().stream()
            .filter(t -> t instanceof Scale)
            .map(t -> (Scale) t)
            .findFirst()
            .orElseGet(() -> {
                Bounds bounds = node.getLayoutBounds();
                // Встановлюємо pivot point в нижній частині вікна
                Scale scale = new Scale(1, 1, bounds.getWidth() / 2, bounds.getHeight() * 0.9);
                node.getTransforms().add(scale);
                return scale;
            });
    }

    private static Translate getOrCreateTranslate(Node node) {
        return node.getTransforms().stream()
            .filter(t -> t instanceof Translate)
            .map(t -> (Translate) t)
            .findFirst()
            .orElseGet(() -> {
                Translate translate = new Translate(0, 0);
                node.getTransforms().add(translate);
                return translate;
            });
    }

    private static EffectContainer getOrCreateEffects(Node node) {
        if (node.getEffect() instanceof Blend blend && 
            blend.getBottomInput() instanceof DropShadow shadow &&
            blend.getTopInput() instanceof GaussianBlur blur) {
            return new EffectContainer(shadow, blur, blend);
        }
        
        // Створюємо новий комбінований ефект
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.25));
        shadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        shadow.setRadius(25.0);
        shadow.setOffsetX(0);
        shadow.setOffsetY(8.0);
        shadow.setSpread(0.1);
        
        GaussianBlur blur = new GaussianBlur();
        blur.setRadius(0.0);
        
        // Комбінуємо shadow і blur через Blend
        Blend blend = new Blend();
        blend.setMode(BlendMode.SRC_OVER);
        blend.setBottomInput(shadow);
        blend.setTopInput(blur);
        
        node.setEffect(blend);
        return new EffectContainer(shadow, blur, blend);
    }

    private static void setupInitialStateFromBottom(Node root, Stage stage) {
        root.setOpacity(0.0);
        
        Scale scale = getOrCreateBottomScale(root);
        scale.setX(0.7);
        scale.setY(0.5);  // Більше стискання по вертикалі
        
        Translate translate = getOrCreateTranslate(root);
        translate.setY(40.0);  // Початкова позиція знизу
        
        stage.show();
        stage.toFront();
        // Встановлюємо початкові ефекти
        EffectContainer effects = getOrCreateEffects(root);
        effects.shadow.setRadius(0.0);
        effects.shadow.setOffsetY(0.0);
        effects.blur.setRadius(12.0);
    }

    private static void resetProperties(Node root, Scale scale, Translate translate, EffectContainer effects) {
        root.setOpacity(1.0);
        scale.setX(1.0);
        scale.setY(1.0);
        translate.setY(0.0);
        
        effects.shadow.setRadius(25.0);
        effects.shadow.setOffsetY(8.0);
        effects.blur.setRadius(0.0);
    }
}