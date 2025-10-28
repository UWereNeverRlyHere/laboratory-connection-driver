package ywh.fx_app.application;

import ywh.fx_app.configs.ApiConfigWindowController;
import ywh.fx_app.device.DevicePrintAndFilesController;
import ywh.fx_app.device.DeviceWindowController;
import ywh.services.data.enums.FileResultActions;
import ywh.services.device.parsers.IParser;
import ywh.services.device.parsers.IParserWithFixedPort;
import ywh.services.device.parsers.ISerialParser;

import java.util.List;

public class UiAccessibilityResolver {
    private UiAccessibilityResolver() {}
    public static void resolve(DeviceWindowController controller) {
        configureApiSection(controller);
        configurePrintAndFilesSection(controller);
        // інші розділи в майбутньому
    }
    public static void resolveForParser(DeviceWindowController controller,IParser parser) {
        if (parser instanceof IParserWithFixedPort iParserWithFixedPort) {
            controller.portField.setDisable(true);
            controller.portField.setText(String.valueOf(iParserWithFixedPort.getDefaultPort()));
        }

        if (parser instanceof ISerialParser ) {
            controller.portField.setDisable(true);
            controller.hostField.setDisable(true);
        }else {
            controller.centerVBox.getChildren().remove(controller.serialConfig);
        }

    }

    private static void configureApiSection(DeviceWindowController controller) {
        List<FileResultActions> allowedActions = AppStaticConfig.ALLOWED_ACTIONS;
        boolean hasApiActions = allowedActions.stream()
                .anyMatch(ApiConfigWindowController.REQUIRED_ACTIONS::contains);

        if (!hasApiActions) {
            controller.centerVBox.getChildren().remove(controller.apiConfig);
        }
    }

    private static void configurePrintAndFilesSection(DeviceWindowController controller) {
        List<FileResultActions> allowedActions = AppStaticConfig.ALLOWED_ACTIONS;
        boolean hasPrintActions = allowedActions.stream()
                .anyMatch(DevicePrintAndFilesController.REQUIRED_ACTIONS::contains);

        if (!hasPrintActions) {
            controller.centerVBox.getChildren().remove(controller.printAndFilesSettings);
        } else if (isOnlyDbfAction(allowedActions)) {
            controller.printAndFilesSettingsController.disablePrintAndFilesFieldsExceptOutput();
        }
    }

    private static boolean isOnlyDbfAction(List<FileResultActions> actions) {
        return actions.size() == 1 && actions.contains(FileResultActions.CREATE_DBF_FILE);
    }

}
