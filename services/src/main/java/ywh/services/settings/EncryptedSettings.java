package ywh.services.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import ywh.services.settings.data.ApiSettings;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.settings.data.ProgramSettings;
import ywh.services.device.IParser;
import ywh.services.exceptions.SettingsRepoException;
import ywh.logging.MainLogger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class EncryptedSettings {
    private EncryptedSettings() {

    }

    @Getter
    private static ProgramSettings cash = new ProgramSettings();
    // Файл, в котором хранятся зашифрованные настройки
    private static final Path CONF = Paths.get("config.dat");

    // Имя алгоритма: AES/GCM без заполнения
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    // Размер IV для GCM (12 байт — рекомендуемое значение)
    private static final int IV_SIZE = 12;
    // Длина аутентификационного тега в битах
    private static final int TAG_LENGTH_BITS = 128;

    // 256-битный ключ (32 байта). В реальных приложениях лучше хранить его в защищённом хранилище!
    private static final byte[] KEY = java.util.HexFormat.of().parseHex(
            "6EA2C3F4B7D8E9A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6E7F8A9B0C1D2E3F4A5"
    );

    // Простой Gson без TypeAdapter - теперь все поля String!
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .addSerializationExclusionStrategy(new ParserExclusionStrategy())
            .addDeserializationExclusionStrategy(new ParserExclusionStrategy())
            .create();

    // Надёжный генератор случайных чисел для IV
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static Optional<DeviceSettings> find(IParser parser) {
        return cash.getDevicesSettings().stream().filter(x ->
                        x.getServiceName().equals(parser.getServiceName()))
                .findFirst();
    }

    public static void save(DeviceSettings cfg) throws SettingsRepoException {
        // Сериализуем настройки в JSON
        try {
            var params = find(cfg.getParser());
            params.ifPresentOrElse(x -> cash.updateDeviceSettings(x, cfg),
                    () -> cash.addDeviceSettings(cfg));

            save();
        } catch (Exception e) {
            throw new SettingsRepoException("[save data error] " + e.getMessage());
        }

    }

    public static void save() throws SettingsRepoException {
        try {
            String jsonString = GSON.toJson(cash);
            byte[] plainText = jsonString.getBytes(StandardCharsets.UTF_8);
            // Генерируем новый случайный IV
            byte[] iv = new byte[IV_SIZE];
            SECURE_RANDOM.nextBytes(iv);

            // Инициализируем шифратор
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Шифруем JSON
            byte[] cipherText = cipher.doFinal(plainText);

            // Объединяем IV и зашифрованный текст: итоговый формат файла = [IV][cipherText]
            byte[] output = new byte[IV_SIZE + cipherText.length];
            System.arraycopy(iv, 0, output, 0, IV_SIZE);
            System.arraycopy(cipherText, 0, output, IV_SIZE, cipherText.length);

            // Записываем данные в файл (создаёт новый или перезаписывает существующий)
            Files.write(CONF, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new SettingsRepoException("[save data error] " + e.getMessage());
        }
    }

    public static DeviceSettings get(IParser parser) {
        if (find(parser).isPresent()) {
            return find(parser).get();
        }
        var device = new DeviceSettings().setParser(parser);
        cash.addDeviceSettingsIfAbsent(device);
        return device;
    }

    public static List<ApiSettings> getApiSettingsList() {
        return cash.getApiSettings();
    }


    public static synchronized void load() throws SettingsRepoException {
        try {
            if (!Files.exists(CONF)) {
                throw new SettingsRepoException("Settings file not found: " + CONF.toAbsolutePath());
            }

            // Читаем данные из файла
            byte[] fileContent = Files.readAllBytes(CONF);
            if (fileContent.length < IV_SIZE) {
                throw new SettingsRepoException("Invalid data file: too short.");
            }

            // Выделяем IV и зашифрованный JSON
            byte[] iv = Arrays.copyOfRange(fileContent, 0, IV_SIZE);
            byte[] cipherText = Arrays.copyOfRange(fileContent, IV_SIZE, fileContent.length);

            // Инициализируем дешифратор
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Дешифруем зашифрованные данные
            byte[] plainText = cipher.doFinal(cipherText);
            String jsonString = new String(plainText, StandardCharsets.UTF_8);

            // Десериализуем JSON в объект CommunicatorParams

            ProgramSettings deserializedSettings = GSON.fromJson(jsonString, ProgramSettings.class);
            cash = deserializedSettings;
        } catch (Exception e) {
            MainLogger.error("[load data error] ", e);
        }
    }
}