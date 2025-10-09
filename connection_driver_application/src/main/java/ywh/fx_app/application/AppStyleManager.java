package ywh.fx_app.application;

import io.github.palexdev.materialfx.theming.base.Theme;
import ywh.logging.MainLogger;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AppStyleManager {
    private AppStyleManager() {
    }

    public static List<Theme> getAllThemes(){
        List<String> cssFiles = Arrays.asList(
                "root.css",
                "app_styles.css",
                "box_styles.css",
                "button_styles.css",
                "combo_box_styles.css",
                "text_fields_styles.css",
                "titled_pane_styles.css"
        );
        return cssFiles.stream()
                .map(fileName -> {
                    final String resourcePath = "/css/" + fileName;
                    URL resourceUrl = AppStyleManager.class.getResource(resourcePath);

                    if (resourceUrl == null) {
                        MainLogger.warn("Custom CSS resource not found and will be skipped: " + resourcePath);
                        return null;
                    }

                    return new Theme() {
                        @Override
                        public String name() {
                            return fileName.substring(0, fileName.lastIndexOf('.'));
                        }

                        @Override
                        public String path() {
                            return resourceUrl.toExternalForm();
                        }

                        @Override
                        public URL get() {
                            // Надаємо URL напряму. Це ключ до успіху в .exe
                            return resourceUrl;
                        }
                    };
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

   /* private static final String STYLES_PATH = "/css/";

    public static void applyDefaultStyle(Scene scene) {
        try {
            URL stylesUrl = AppStyleManager.class.getResource(STYLES_PATH);
            if (stylesUrl == null) return;

            // Визначаємо, у якій файловій системі лежить ресурс
            URI uri = stylesUrl.toURI();
            Path dir;
            if ("jrt".equals(uri.getScheme())) {          // запущено з modular-runtime
                dir = FileSystems.getFileSystem(uri).getPath(uri.getPath());
            } else {                                      // класичний launch із IDE/JAR
                dir = Paths.get(uri);
            }

            // Очищаємо існуючі стилі
           // scene.getStylesheets().clear();

            // Перебираємо всі .css
            try (Stream<Path> paths = Files.list(dir)) {
                paths.filter(p -> p.getFileName().toString().endsWith(".css"))
                        .sorted(Comparator
                                .comparing((Path p) -> !p.getFileName().toString().equals("root.css"))
                                .thenComparing(Path::getFileName))
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            if (fileName.contains("tray")) return;          // пропускаємо tray-стилі
                            String cssURL = Objects.requireNonNull(
                                    AppStyleManager.class.getResource(STYLES_PATH + fileName)
                            ).toExternalForm();
                            scene.getStylesheets().add(cssURL);
                        });
            }

        } catch (Exception e) {
            e.printStackTrace();   // можете замінити на власний логер
        }
    }*/


}