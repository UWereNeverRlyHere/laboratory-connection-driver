package ywh.fx_app.application;

import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import io.github.palexdev.materialfx.theming.base.Theme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import ywh.commons.DeferredFileDeleter;
import ywh.fx_app.app_data.ImageLoader;
import ywh.fx_app.clarification.ClarificationProvider;
import ywh.fx_app.device.DeviceFactory;
import ywh.fx_app.tray.TrayManager;
import ywh.services.exceptions.SettingsRepoException;
import ywh.services.settings.EncryptedSettings;
import ywh.logging.MainLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DriverApp extends Application {

    private ApplicationInstanceService instanceService;
    @SuppressWarnings("FieldCanBeLocal")
    private Stage primaryStage;


    public static void main(String[] args) {
        launch(args);
    }

    private List<Theme> findAllCssThemes() {
        final String stylesPath = "/css/";
        try {
            URL stylesUrl = getClass().getResource(stylesPath);
            if (stylesUrl == null) {
                MainLogger.warn("CSS resource directory not found: " + stylesPath);
                return Collections.emptyList();
            }

            URI uri = stylesUrl.toURI();
            Path dir;

            if ("jar".equals(uri.getScheme())) {
                try {
                    dir = FileSystems.getFileSystem(uri).getPath(stylesPath);
                } catch (FileSystemAlreadyExistsException e) {
                    dir = FileSystems.getFileSystem(uri).getPath(stylesPath);
                } catch (Exception e) {
                    dir = FileSystems.newFileSystem(uri, Collections.emptyMap()).getPath(stylesPath);
                }
            } else {
                dir = Paths.get(uri);
            }

            try (Stream<Path> paths = Files.walk(dir, 1)) {
                return paths
                        .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(".css"))
                        .sorted(Comparator
                                .comparing((Path p) -> !p.getFileName().toString().equals("root.css"))
                                .thenComparing(Path::getFileName))
                        .map(p -> {
                            String fileName = p.getFileName().toString();
                            if (fileName.contains("tray")) return null;

                            URL resourceUrl = getClass().getResource(stylesPath + fileName);
                            if (resourceUrl == null) return null;

                            return new Theme() {
                                @Override
                                public String name() {
                                    return fileName.substring(0, fileName.lastIndexOf('.'));
                                }

                                @Override
                                public String path() {
                                    // Цей метод все ще потрібен для реалізації інтерфейсу,
                                    // але MaterialFX буде використовувати наш get()
                                    return resourceUrl.toExternalForm();
                                }

                                @Override
                                public URL get() {
                                    // Надаємо URL напряму, оминаючи проблемну логіку за замовчуванням
                                    return resourceUrl;
                                }
                            };
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (URISyntaxException | IOException e) {
            MainLogger.error("Failed to read CSS resources", e);
            return Collections.emptyList();
        }
    }


    @Override
    public void init() throws Exception {
        super.init();
        ImageLoader.warmUpAll();
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .themes(AppStyleManager.getAllThemes().toArray(new Theme[0]))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();


        // Ініціалізуємо сервіс single instance
        instanceService = new ApplicationInstanceService(AppStaticConfig.APP_NAME);
        // Перевіряємо single instance
        if (!instanceService.acquireLock()) {
            // Показуємо повідомлення та намагаємося активувати існуюче вікно
            instanceService.notifyExistingInstance();
            Platform.exit();
            System.exit(0);
        }


        Platform.setImplicitExit(false);

        MainLogger.info("Application initialized successfully");
    }

    @Override
    public void start(Stage stage) throws SettingsRepoException {
        primaryStage = stage;
        primaryStage.setWidth(1);
        primaryStage.setHeight(1);
        primaryStage.setOpacity(0);
        primaryStage.setX(-1000);
        primaryStage.setY(-1000);
        primaryStage.setResizable(true);
        primaryStage.sizeToScene();
        instanceService.startActivationMonitoring(stage);
        // Tray має завантажитися до того, як завантажаться контроллери
        EncryptedSettings.load();
        TrayManager.createTray(primaryStage, this::exitApplication);
        try {
            DeviceFactory.initialize();
        } catch (Throwable t) {
            MainLogger.error("DeviceFactory failed", t);
        }
        MainLogger.info("Application started successfully");
    }

    /**
     * Правильний метод для завершення додатку
     */
    private void exitApplication() {
        MainLogger.info("Application closing...");
        DeviceFactory.shutdownAllDevices();
        // Спочатку прибираємо трей
        TrayManager.removeTray();
        ClarificationProvider.shutdown();
        // Потім звільняємо ресурси
        if (instanceService != null) {
            instanceService.releaseLock();
        }
        // Завершуємо JavaFX
        Platform.exit();
        DeferredFileDeleter.close();

        // Завершуємо JVM
        System.exit(0);
    }

    @Override
    public void stop() throws Exception {
        MainLogger.info("Application stopping...");
        super.stop();
    }
}