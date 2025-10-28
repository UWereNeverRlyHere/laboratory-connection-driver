package ywh.services.data.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.services.communicator.ICommunicator;
import ywh.services.device.IClarificationProvider;
import ywh.services.device.parsers.IParser;
import ywh.services.settings.data.DeviceSettings;
import ywh.logging.DeviceLogger;

import java.util.Optional;

@Data
@NoArgsConstructor
public class DeviceConfig {

    private ICommunicator communicator;
    private IClarificationProvider clarificationProvider;
    private IParser parser;
    private DeviceLogger logger;
    private DeviceSettings deviceSettings;

    // ✅ Fluent Builder Methods
    public DeviceConfig setCommunicator(ICommunicator communicator) {
        this.communicator = communicator;
        return this;
    }

    public DeviceConfig setClarificationProvider(IClarificationProvider clarificationProvider) {
        this.clarificationProvider = clarificationProvider;
        return this;
    }

    public Optional<IClarificationProvider> getClarificationProvider() {
        return Optional.ofNullable(clarificationProvider);
    }

    public DeviceConfig setParser(IParser parser) {
        this.parser = parser;
        return this;
    }

    public DeviceConfig setLogger(DeviceLogger logger) {
        this.logger = logger;
        return this;
    }

    public DeviceConfig setDeviceSettings(DeviceSettings deviceSettings) {
        this.deviceSettings = deviceSettings;
        return this;
    }

    // ✅ Static Builder Method (якщо потрібно)
    public static DeviceConfig builder() {
        return new DeviceConfig();
    }

    // ✅ Build method для завершення
    public DeviceConfig build() {
        return this;
    }
}