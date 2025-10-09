package ywh.services.settings.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class ProgramSettings {
    private List<ApiSettings> apiSettings = new ArrayList<>();
    private EmailSettings emailSettings;
    private List<DeviceSettings> devicesSettings = new ArrayList<>();

    public void addDeviceSettings(DeviceSettings deviceSettings) {
        devicesSettings.add(deviceSettings);
    }

    public void addDeviceSettingsIfAbsent(DeviceSettings deviceSettings) {
        try {
            var search = devicesSettings.stream().filter(device ->
                            device.getCachedParser().getServiceName().equals(deviceSettings.getCachedParser().getServiceName()))
                    .findAny();
            if (search.isEmpty())
                devicesSettings.add(deviceSettings);
        } catch (Exception e) {
            devicesSettings.add(deviceSettings);
        }

    }

    public void updateDeviceSettings(int index, DeviceSettings deviceSettings) {
        devicesSettings.set(index, deviceSettings);
    }

    public void updateDeviceSettings(DeviceSettings deviceSettings, DeviceSettings newDeviceSettings) {
        devicesSettings.set(devicesSettings.indexOf(deviceSettings), newDeviceSettings);
    }
    /**
     * Автоматично збирає всі унікальні ApiSettings з devicesSettings і додає їх до загального списку apiSettings
     * Викликається після збереження налаштувань пристроїв
     */
    public void collectUniqueApiSettingsFromDevices() {
        if (devicesSettings == null || devicesSettings.isEmpty()) {
            return;
        }

        // Створюємо Set для перевірки унікальності (на основі URL та timeout)
        Set<String> existingApiKeys = apiSettings.stream()
                .map(this::createApiKey)
                .collect(Collectors.toSet());

        // Збираємо нові унікальні ApiSettings з пристроїв
        List<ApiSettings> newApiSettings = devicesSettings.stream()
                .map(DeviceSettings::getApiSettings)
                .filter(Objects::nonNull)
                .filter(apiSetting -> {
                    String key = createApiKey(apiSetting);
                    return !existingApiKeys.contains(key);
                })
                .distinct()
                .collect(Collectors.toList());

        apiSettings.addAll(newApiSettings);


    }

    /**
     * Створює унікальний ключ для ApiSettings на основі URL та timeout
     */
    private String createApiKey(ApiSettings apiSettings) {
        return String.format("%s|%s|%d",
                apiSettings.getResultUrl() != null ? apiSettings.getResultUrl() : "",
                apiSettings.getOrderUrl() != null ? apiSettings.getOrderUrl() : "",
                apiSettings.getTimeOut());
    }

    /**
     * Очищає дублікати в apiSettings (може використовуватися для очищення після ручного редагування)
     */
    public void removeDuplicateApiSettings() {
        if (apiSettings == null || apiSettings.size() <= 1) {
            return;
        }

        int originalSize = apiSettings.size();

        apiSettings = new ArrayList<>(apiSettings.stream()
                .collect(Collectors.toMap(
                        this::createApiKey,
                        Function.identity(),
                        (existing, replacement) -> existing)) // При дублікатах залишаємо перший
                .values());


    }

}
