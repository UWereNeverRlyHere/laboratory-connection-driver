package ywh.fx_app.device;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import ywh.fx_app.app_data.FXMLLoaders;
import ywh.fx_app.app_data.ImageLoader;
import ywh.fx_app.app_utils.StageUtils;
import ywh.fx_app.application.AppStaticConfig;
import ywh.services.device.Device;
import ywh.services.device.parsers.IParser;
import ywh.logging.MainLogger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceFactory {
    @Getter
    private static final Map<IParser, DeviceChain> DEVICE_CHAINS = new ConcurrentHashMap<>();

    protected record DeviceChain(Stage stage,
                                 DeviceWindowController controller,
                                 DeviceManager manager,
                                 Device.DeviceHandle device) {
    }

    private DeviceFactory() {
    }


    public static void openDeviceWindow(IParser parser) {


        DeviceChain chain = DEVICE_CHAINS.get(parser);
        if (chain != null) {
            Stage stage = chain.stage();
            if (stage.isShowing() && stage.isIconified()) {
                stage.setIconified(false);
            } else if (!stage.isShowing()) {
                stage.show();
            }
            stage.toFront();
        } else {
            MainLogger.error("No device chain found for parser: " + parser.getName());
        }

    }

    public static synchronized void initialize() {
        if (!DEVICE_CHAINS.isEmpty()) return;
        MainLogger.info("Initializing DeviceFactory...");
        AppStaticConfig.PARSERS.forEach(parser -> {
            try {
                DEVICE_CHAINS.put(parser, loadSingleChain(parser));
            } catch (IOException e) {
                MainLogger.error("Failed to load device window: " + e.getMessage(), e);
            }
        });
        MainLogger.info("DeviceFactory initialized with " + DEVICE_CHAINS.size() + " devices");
    }

    private static DeviceChain loadSingleChain(IParser parser) throws IOException {
        FXMLLoader fxmlLoader = FXMLLoaders.DEVICE.getLoader();
        Scene scene = new Scene(fxmlLoader.load());

        DeviceWindowController controller = fxmlLoader.getController();

        Stage stage = new Stage();

        stage.setScene(scene);
        stage.setResizable(true);
        stage.setTitle("[" + parser.getServiceName() + "]");
        stage.getIcons().add(ImageLoader.APP_IMAGE.getFxImage());
        StageUtils.setMinStageBoundsByScreenSize(stage, 0.22, 0.3);
        DeviceManager manager = new DeviceManager(controller, parser);

        return new DeviceChain(stage, controller, manager, manager.getDevice());
    }


    public static void shutdownAllDevices() {
        MainLogger.info("Shutting down all devices...");
        DEVICE_CHAINS.values().forEach(chain -> {
            try {
                if (chain.device() != null) {
                    Device.closeDevice(chain.device());
                }
                chain.stage().close();
            } catch (Exception e) {
                MainLogger.error("Error shutting down device", e);
            }
        });
        DEVICE_CHAINS.clear();
    }
}
