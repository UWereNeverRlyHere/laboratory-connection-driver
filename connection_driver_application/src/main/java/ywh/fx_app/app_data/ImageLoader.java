package ywh.fx_app.app_data;

import ywh.fx_app.tray.TrayManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageLoader {
    // Публічні константи-«хендли» із тим самим API: APP_IMAGE.getFxImage()
    public static final Icon SILENT_PRINT_ON = new Icon("silent_print_on.png");
    public static final Icon SILENT_PRINT_OFF = new Icon("silent_print_off.png");
    public static final Icon APP_IMAGE = new Icon("connection driver icon_big.png");
    public static final Icon PRINT_FROM_PDF = new Icon("print_from_pdf.png");
    public static final Icon PRINT_FROM_DOCX = new Icon("print_from_docx.png");

    // Потокобезпечні кеші
    private static final ConcurrentHashMap<String, javafx.scene.image.Image> FX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, java.awt.Image> AWT_CACHE = new ConcurrentHashMap<>();

    private ImageLoader() {
    }

    // Обгортка над ім’ям ресурсу з ледачим завантаженням і локальним посиланням
    public static final class Icon {
        private final String fileName;
        // Локальні volatile-посилання прискорюють повторні звернення в межах одного хендла
        private volatile javafx.scene.image.Image fxImage;
        private volatile java.awt.Image awtImage;

        private Icon(String fileName) {
            this.fileName = fileName;
        }

        public javafx.scene.image.Image getFxImage() {
            javafx.scene.image.Image img = fxImage;
            if (img == null) {
                img = FX_CACHE.computeIfAbsent(fileName, fn -> {
                    try (var is = ImageLoader.class.getResourceAsStream("/images/" + fn)) {
                        return new javafx.scene.image.Image(
                                Objects.requireNonNull(is, "Image not found: " + fn));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                fxImage = img;
            }
            return img;
        }

        public java.awt.Image getAwtImage() {
            java.awt.Image img = awtImage;
            if (img == null) {
                img = AWT_CACHE.computeIfAbsent(fileName, fn ->
                        java.awt.Toolkit.getDefaultToolkit().getImage(
                                Objects.requireNonNull(TrayManager.class.getResource("/images/" + fn),
                                        "Image not found: " + fn)));
                awtImage = img;
            }
            return img;
        }
    }

    // Додатково: прогрів усіх відомих іконок (опційно викликати на старті додатку)
    public static void warmUpAll() {
        try {
            for (Field field : ImageLoader.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && Modifier.isPublic(field.getModifiers())
                        && Icon.class.equals(field.getType())) {
                    Icon icon = (Icon) field.get(null); // public static field
                    if (icon != null) {
                        icon.getFxImage();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to warm-up images via reflection", e);
        }
    }


    // Очистка кешів (напр., для тестів або при зміні теми/ресурсів)
    public static void clearCaches() {
        FX_CACHE.clear();
        AWT_CACHE.clear();
    }
}
