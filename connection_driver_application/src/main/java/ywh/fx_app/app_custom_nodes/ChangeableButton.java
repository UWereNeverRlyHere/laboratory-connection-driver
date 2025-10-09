package ywh.fx_app.app_custom_nodes;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ywh.fx_app.app_data.ImageLoader;
import ywh.fx_app.app_utils.Animations;

import java.io.InputStream;
import java.util.Objects;
@EqualsAndHashCode(callSuper = true)
@Data
public class ChangeableButton extends MFXButton {
    @Getter
    private boolean state;

    // Для FXML: шляхи до ресурсів (наприклад, "/images/print_from_pdf.png")
    private String firstImageUrl;
    private String secondImageUrl;

    // Програмний спосіб: через ImageLoader
    private ywh.fx_app.app_data.ImageLoader.Icon firstImageLoader;
    private ywh.fx_app.app_data.ImageLoader.Icon secondImageLoader;

    private String firstText;
    private String secondText;

    private double fitWidth = 20;
    private double fitHeight = 20;
    private boolean preserveRatio = true;

    private final Tooltip innerTooltip = new Tooltip();
    private final ImageView imageView = new ImageView();

    public ChangeableButton() {
        super();
        setTooltip(innerTooltip);
        setGraphic(imageView);
        addDefaultAction();
        // Розмір іконки можна підлаштувати за потреби:
        imageView.setPreserveRatio(preserveRatio);
        imageView.setFitHeight(fitHeight);
        imageView.setFitWidth(fitWidth);
        imageView.setPickOnBounds(true);
        setBackground(null);
        // Додаємо дефолтний стиль
        if (!getStyleClass().contains("image-button")) {
            getStyleClass().add("image-button");
        }
    }

    public void setFitWidth(double fitWidth) {
        this.fitWidth = fitWidth;
        imageView.setFitWidth(fitWidth);
    }

    public void setFitHeight(double fitHeight) {
        this.fitHeight = fitHeight;
        imageView.setFitHeight(fitHeight);
    }

    public void setPreserveRatio(boolean preserveRatio) {
        this.preserveRatio = preserveRatio;
        imageView.setPreserveRatio(preserveRatio);
    }


    public ChangeableButton first(ywh.fx_app.app_data.ImageLoader.Icon image, String text) {
        this.firstImageLoader = image;
        this.firstText = text;
        return this;
    }

    // Аналог колишнього second(ImageLoader, text)
    public ChangeableButton second(ywh.fx_app.app_data.ImageLoader.Icon image, String text) {
        this.secondImageLoader = image;
        this.secondText = text;
        return this;
    }

    // Виклик для встановлення іконки відповідно до поточного стану (true -> first, false -> second)
    public void setImage() {
        if (state) {
            imageView.setImage(loadFirstImage());
            innerTooltip.setText(firstText);
        } else {
            imageView.setImage(loadSecondImage());
            innerTooltip.setText(secondText);
        }
    }

    // Перемикання стану з анімацією як раніше
    public void toggle() {
        state = !state;
        Animations.fadeOutFadeIn(imageView, imageView, 150, event -> setImage());
    }

    // Дефолтні дії кліку
    public ChangeableButton addDefaultAction(Runnable action) {
        setOnAction(e -> {
            action.run();
            toggle();
        });
        return this;
    }

    public ChangeableButton addDefaultAction() {
        setOnAction(e -> toggle());
        return this;
    }

    // Додатково: можливість програмно задати підписи тултіпа
    public ChangeableButton firstText(String text) {
        this.firstText = text;
        return this;
    }

    public ChangeableButton secondText(String text) {
        this.secondText = text;
        return this;
    }

    // Для FXML: сеттери, які SceneBuilder/FXML можуть викликати
    public void setFirstImageUrl(String firstImageUrl) {
        this.firstImageUrl = firstImageUrl;
        // Якщо програмно не задано через ImageLoader — використовуємо URL
        if (imageView.getImage() == null) {
            setImage();
        }
    }

    public void setSecondImageUrl(String secondImageUrl) {
        this.secondImageUrl = secondImageUrl;
        if (imageView.getImage() == null) {
            setImage();
        }
    }

    public void setFirstText(String text) {
        this.firstText = text;
        if (state) {
            innerTooltip.setText(text);
        }
    }

    public void setSecondText(String text) {
        this.secondText = text;
        if (!state) {
            innerTooltip.setText(text);
        }
    }

    // Для програмного задання через ImageLoader
    public void setFirstImageLoader(ywh.fx_app.app_data.ImageLoader.Icon loader) {
        this.firstImageLoader = loader;
        setImage();
    }

    public void setSecondImageLoader(ywh.fx_app.app_data.ImageLoader.Icon loader) {
        this.secondImageLoader = loader;
        setImage();
    }

    // Опційно: дозволити зовні ставити стан і одразу оновити картинку
    public void setState(boolean state) {
        this.state = state;
        setImage();
    }

    // =========================================
    // ВНУТРІШНІ МЕТОДИ ЗАВАНТАЖЕННЯ ЗОБРАЖЕНЬ
    // =========================================
    private Image loadFirstImage() {
        if (firstImageLoader != null) {
            return firstImageLoader.getFxImage();
        }
        return loadFromUrl(firstImageUrl);
    }

    private Image loadSecondImage() {
        if (secondImageLoader != null) {
            return secondImageLoader.getFxImage();
        }
        return loadFromUrl(secondImageUrl);
    }

    private Image loadFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        // Підтримка шляхів ресурсів у classpath ("/images/..")
        InputStream is = getClass().getResourceAsStream(url);
        if (is != null) {
            return new Image(is);
        }
        // Якщо раптом передали абсолютний/файловий або http шлях — нехай Image розбере сам
        return new Image(Objects.requireNonNull(url));
    }
}