package ywh.fx_app.device;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.Getter;
import ywh.fx_app.app_binders.SettingsBinder;
import ywh.fx_app.app_exceptions.ApplicationException;
import ywh.fx_app.app_exceptions.SettingsValidationException;
import ywh.fx_app.app_managers.CheckManager;
import ywh.fx_app.application.UiAccessibilityResolver;
import ywh.fx_app.clarification.ClarificationProvider;
import ywh.fx_app.tray.TrayManager;
import ywh.services.data.enums.DeviceStatus;
import ywh.services.data.models.DeviceConfig;
import ywh.services.device.Device;
import ywh.services.device.parsers.IParser;
import ywh.services.device.parsers.IParserWithFixedPort;
import ywh.services.printing.PrintersService;
import ywh.services.printing.PrintingMethod;
import ywh.services.settings.EncryptedSettings;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.settings.data.PrintSettings;
import ywh.logging.DeviceLogger;
import ywh.logging.MainLogger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

public class DeviceManager {
    private final DeviceWindowController controller;
    private DeviceSettings deviceSettings = new DeviceSettings();

    @Getter
    private Device.DeviceHandle device;
    private IParser parser;
    protected DeviceStatusUtil statusUtil;
    private final CheckManager checkManager = new CheckManager();


    public DeviceManager(DeviceWindowController controller, IParser parser) {
        this.controller = controller;
        this.controller.setDeviceManager(this);
        this.controller.printAndFilesSettingsController.setDeviceManager(this);
        this.controller.printAndFilesSettingsController.setParentController(controller);
        initializeAndStart(parser);
        initializeCheckers();
        UiAccessibilityResolver.resolve(controller);
    }


    private void initializeAndStart(IParser parser) {
        this.parser = parser;
        var trayStatusLbl = TrayManager.getController().createNewStatusLbl(parser, controller.statusCircle);
        statusUtil = DeviceStatusUtil.bindTo(controller.statusLabel, controller.statusCircle, trayStatusLbl).setStopped();
        loadFromSettingsAndSetFields();
        try {
            deviceSettings.setParser(parser);
            restartDeviceAndSetStatus();
            MainLogger.info("Device autostart completed");
        } catch (Exception e) {
            MainLogger.error("Помилка під час автозапуску пристрою", e);
        }

        createCloseAction();
    }


    private void initializeCheckers() {

        checkManager
             /*   .add("ftp", () -> {
                    deviceSettings.getFileResultProcessorSettings().setUseFtp(!controller.printAndFilesSettingsController.outputPathField.getFieldText().contains("\\:"));
                })*/
                .add("templateFile", () -> {
                    var file = new File(controller.printAndFilesSettingsController.templateFileField.getFieldText());
                    if (!file.exists())
                        throw new SettingsValidationException("Вказаний файл шаблону не знайдено");
                });
    }

    private void createCloseAction() {
        controller.getRoot().getScene().getWindow().setOnCloseRequest(event -> {
            try {
                boolean hasChanges = SettingsBinder.hasChanges(controller, deviceSettings) ||
                        SettingsBinder.hasChanges(controller.printAndFilesSettingsController, deviceSettings);

                if (hasChanges) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Підтвердження дії");
                    alert.setHeaderText("У вас є незбережені зміни");
                    alert.setContentText("Бажаєте зберегти зміни перед закриттям?");

                    ButtonType saveButton = new ButtonType("Зберегти");
                    ButtonType discardButton = new ButtonType("Не зберігати");
                    ButtonType cancelButton = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);


                    alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
                        if (result.get() == saveButton) {
                            saveAndStart();
                        } else if (result.get() == discardButton) {
                            SettingsBinder.fillAll(controller, deviceSettings);
                            SettingsBinder.fillAll(controller.printAndFilesSettingsController, deviceSettings);
                        } else {
                            event.consume();
                            return;
                        }
                    }
                }
                ((Stage) event.getSource()).hide();
                event.consume();
            } catch (Exception e) {
                MainLogger.error("Помилка при закритті вікна", e);
                event.consume();
                ((Stage) event.getSource()).hide();
            }
        });
    }

    private void setDefaultValues() {
        // Встановлення принтера за замовчуванням з Windows
        try {
            String defaultPrinter = PrintersService.getDefaultPrinterViaPowerShell();
            if (defaultPrinter != null && !defaultPrinter.isEmpty()) {
                // Оновлюємо параметри друку із принтером за замовчуванням
                PrintSettings defaultPrintSettings = new PrintSettings(
                        defaultPrinter,
                        PrintingMethod.AUTO,
                        true,
                        true
                );
                deviceSettings.setPrintSettings(defaultPrintSettings);
            }
        } catch (Exception e) {
            MainLogger.error("Не вдалося отримати принтер за замовчуванням", e);
        }
    }

    private void loadFromSettingsAndSetFields() {
        try {
            deviceSettings = EncryptedSettings.get(parser);
            controller.printAndFilesSettingsController.setDeviceSettings(deviceSettings);

            SettingsBinder.fillAll(controller, deviceSettings);
            SettingsBinder.fillAll(controller.printAndFilesSettingsController, deviceSettings);
            SettingsBinder.fillAll(controller.apiConfigController, deviceSettings);
            SettingsBinder.fillAll(controller.serialConfigController, deviceSettings);

            controller.serialConfigController.selectPort(deviceSettings.getCommunicatorSettings().getSerialParams().getPortName());
            controller.printAndFilesSettingsController.refreshPrinters();
            controller.printAndFilesSettingsController.setPrinterFromSettings();

            UiAccessibilityResolver.resolveForParser(controller, parser);

        } catch (Exception e) {
            // Якщо перший запуск або помилка в налаштуваннях - встановлюємо значення за замовчуванням
            setDefaultValues();
            MainLogger.error("Помилка під час читання налаштувань, використовуються значення за замовчуванням", e);
        }

    }

    public void saveAndStart() throws SettingsValidationException, ApplicationException {
        try {
            checkManager.check();
            SettingsBinder.commitAll(controller, deviceSettings);
            SettingsBinder.commitAll(controller.printAndFilesSettingsController, deviceSettings);
            SettingsBinder.commitAll(controller.apiConfigController, deviceSettings);
            SettingsBinder.commitAll(controller.serialConfigController, deviceSettings);

            // Отримуємо глобальні налаштування і збираємо унікальні API налаштування
            var programSettings = EncryptedSettings.getCash();
            if (programSettings != null) {
                programSettings.collectUniqueApiSettingsFromDevices();
                EncryptedSettings.save();
            }
            refreshAllConfigs();

            restartDeviceAndSetStatus();
        } catch (SettingsValidationException e) {
            throw e; // Проксуємо для обробки в контролері
        } catch (Exception e) {
            throw new ApplicationException("Виникла помилка під час запуску: " + e.getMessage(), e);
        }
    }


    private void refreshAllConfigs() throws SettingsValidationException {
        Map<IParser, DeviceFactory.DeviceChain> deviceChains = DeviceFactory.getDEVICE_CHAINS();
        for (Map.Entry<IParser, DeviceFactory.DeviceChain> entry : deviceChains.entrySet()) {
            IParser key = entry.getKey();
            DeviceFactory.DeviceChain chain = entry.getValue();
            chain.controller().apiConfigController.refreshUrls();
            SettingsBinder.fillAll(chain.controller().apiConfigController, chain.manager().deviceSettings);
        }
    }


    protected void restartDeviceAndSetStatus() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        stopDevice();
        // Тепер створюємо нові компоненти
        var logger = DeviceLogger.tryToCreateOrGetDefault(deviceSettings.getLogFileName(), deviceSettings.getParser().getCharset());
        var communicator = parser.createDefaultCommunicator(deviceSettings.getCommunicatorSettings(), logger);
        statusUtil.setStarting();
        var deviceConfig = new DeviceConfig()
                .setCommunicator(communicator)
                .setParser(parser)
                .setLogger(logger)
                .setClarificationProvider(deviceSettings.isClarificationWindow() ? Optional.of(new ClarificationProvider()) : Optional.empty())
                .setDeviceSettings(deviceSettings);
        device = Device.createAndStart(deviceConfig);
        statusUtil.listenTo(device.device());
    }

    public void stopDevice() {
        if (device == null) return;
        Device.closeDevice(device);
        device = null;
        statusUtil.setStatus(DeviceStatus.STOPPED);

    }


/*
  !!!JFX example!!!
  private void createCloseAction() {
        controller.getRoot().getScene().getWindow().setOnCloseRequest(event -> {
            try {
                boolean hasChanges = SettingsBinder.hasChanges(controller, deviceSettings) ||
                        SettingsBinder.hasChanges(controller.printAndFilesSettingsController, deviceSettings);

                if (hasChanges) {
                    // Створюємо JFXAlert замість стандартного Alert
                    JFXAlert<ButtonType> alert = new JFXAlert<>((Stage) event.getSource());
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setOverlayClose(false);

                    // Контент діалогу
                    JFXDialogLayout layout = new JFXDialogLayout();
                    layout.setHeading(new Text("Незбережені зміни"));
                    layout.setBody(new Text("У вас є незбережені зміни. Бажаєте зберегти зміни перед закриттям?"));

                    // Кнопки
                    JFXButton saveButton = new JFXButton("Зберегти");
                    saveButton.getStyleClass().add("dialog-accept");

                    JFXButton discardButton = new JFXButton("Не зберігати");
                    discardButton.getStyleClass().add("dialog-cancel");

                    JFXButton cancelButton = new JFXButton("Скасувати");
                    cancelButton.getStyleClass().add("dialog-cancel");

                    // Обробники кнопок
                    saveButton.setOnAction(e -> {
                        try {
                            saveAndStart();
                            ((Stage) event.getSource()).hide();
                            alert.hideWithAnimation();
                        } catch (Exception ex) {
                            MainLogger.error("Помилка збереження", ex);
                        }
                    });

                    discardButton.setOnAction(e -> {
                        try {
                            SettingsBinder.fillAll(controller, deviceSettings);
                            SettingsBinder.fillAll(controller.printAndFilesSettingsController, deviceSettings);
                            ((Stage) event.getSource()).hide();
                            alert.hideWithAnimation();
                        } catch (Exception ex) {
                            MainLogger.error("Помилка відкату", ex);
                        }
                    });

                    cancelButton.setOnAction(e -> alert.hideWithAnimation());

                    layout.setActions(saveButton, discardButton, cancelButton);
                    alert.setContent(layout);
                    alert.show();
                } else {
                    // Якщо змін немає - просто ховаємо
                    ((Stage) event.getSource()).hide();
                }

                event.consume();

            } catch (Exception e) {
                MainLogger.error("Помилка при закритті вікна", e);
                event.consume();
                ((Stage) event.getSource()).hide();
            }
        });
    }*/

}