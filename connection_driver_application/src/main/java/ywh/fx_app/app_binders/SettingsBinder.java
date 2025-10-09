package ywh.fx_app.app_binders;

import ywh.fx_app.app_exceptions.SettingsValidationException;
import ywh.logging.MainLogger;
import ywh.services.settings.data.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsBinder {

    /**
     * Кеш метаданих біндингів для кожного класу контролера
     * Сканування аннотацій відбувається лише один раз на клас
     */
    private static final ConcurrentHashMap<Class<?>, List<BindingInfo>> BINDING_CACHE = new ConcurrentHashMap<>();

    /**
     * Заполняем все поля контроллера из модели (з кешуванням метаданих)
     */
    public static void fillAll(Object controller, Object model) throws SettingsValidationException {
        List<BindingInfo> bindings = getBindingsForClass(controller.getClass());

        for (BindingInfo binding : bindings) {
            try {
                // Определяем правильную модель для биндинга
                Object actualModel = getActualModel(model, binding.targetModel);

                if (actualModel == null) {
                    continue;
                }

                // Викликаємо fill з кешованими метаданими
                binding.binder.fill(controller, actualModel, binding.field, binding.annotation);

            } catch (SettingsValidationException sve) {
                throw sve;
            } catch (Exception ex) {
                throw new SettingsValidationException(
                        "Помилка при заповненні поля " + binding.field.getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Собираем все поля контроллера обратно в модель (з кешуванням метаданих)
     */
    public static void commitAll(Object controller, Object model) throws SettingsValidationException {
        List<BindingInfo> bindings = getBindingsForClass(controller.getClass());

        for (BindingInfo binding : bindings) {
            try {
                // Определяем правильную модель для биндинга
                Object actualModel = getActualModel(model, binding.targetModel);

                if (actualModel == null) {

                    continue;
                }

                // Викликаємо commit з кешованими метаданими
                binding.binder.commit(controller, actualModel, binding.field, binding.annotation);

            } catch (SettingsValidationException sve) {
                throw sve;
            } catch (Exception ex) {
                throw new SettingsValidationException(
                        "Помилка при збереженні поля " + binding.field.getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

    public static boolean hasChanges(Object controller, Object model) {
        List<BindingInfo> bindings = getBindingsForClass(controller.getClass());

        for (BindingInfo binding : bindings) {
            try {
                Object actualModel = getActualModel(model, binding.targetModel);

                if (actualModel == null) {
                    continue;
                }

                if (binding.binder.hasChanges(controller, actualModel, binding.field, binding.annotation)) {
                    return true; // Знайшли хоча б одну зміну
                }

            } catch (Exception ex) {
                MainLogger.error("Помилка при перевірці змін для поля " + binding.field.getName(), ex);
                return true;
            }
        }

        return false; // Жодних змін не знайдено
    }

    /**
     * Отримує список біндингів для класу (з кешуванням)
     * При першому виклику сканує аннотації, наступні виклики використовують кеш
     */
    private static List<BindingInfo> getBindingsForClass(Class<?> controllerClass) {
        return BINDING_CACHE.computeIfAbsent(controllerClass, SettingsBinder::scanBindings);
    }

    /**
     * Сканує клас контролера і створює список біндингів (викликається лише один раз на клас)
     */
    private static List<BindingInfo> scanBindings(Class<?> controllerClass) {
        List<BindingInfo> bindings = new ArrayList<>();


        for (Field field : controllerClass.getDeclaredFields()) {
            SettingsBind annotation = field.getAnnotation(SettingsBind.class);
            if (annotation == null) continue;

            try {
                // Отримуємо біндер з enum
                PropertyBinder binder = annotation.binder().getBinder();
                Class<?> targetModel = annotation.targetModel();

                // Створюємо інформацію про біндинг
                BindingInfo bindingInfo = new BindingInfo(field, annotation, binder, targetModel);
                bindings.add(bindingInfo);


            } catch (Exception e) {
                MainLogger.error("Помилка при створенні біндинга для поля", e);
            }
        }

        return bindings;
    }

    private static Object getActualModel(Object model, Class<?> targetModelClass) {
        // Якщо переданна модель відповідає цільовому класу - повертаємо її
        if (targetModelClass.isAssignableFrom(model.getClass())) {
            return model;
        }

        // Якщо переданна модель - DeviceSettings, пробуємо знайти потрібну вкладену модель
        if (model instanceof DeviceSettings deviceSettings) {
            if (targetModelClass == CommunicatorSettings.class) {
                return deviceSettings.getCommunicatorSettings();
            }
            if (targetModelClass == PrintSettings.class) {
                return deviceSettings.getPrintSettings();
            }
            if (targetModelClass == FileResultProcessorSettings.class) {
                return deviceSettings.getFileResultProcessorSettings();
            }
            if (targetModelClass == ApiSettings.class) {
                return deviceSettings.getApiSettings();
            }
            // Якщо потрібен сам DeviceSettings - повертаємо його
            if (targetModelClass == DeviceSettings.class) {
                return deviceSettings;
            }
            if (targetModelClass == FtpSettings.class) {
                if (deviceSettings.getFileResultProcessorSettings() == null) {
                    deviceSettings.setFileResultProcessorSettings(new FileResultProcessorSettings());
                }
                return deviceSettings.getFileResultProcessorSettings().getFtpSettings();
            }
        }

        return null; // Модель не знайдена
    }


    /**
     * Очищає кеш біндингів (може знадобитися для тестів або hot-reload)
     */
    public static void clearCache() {
        BINDING_CACHE.clear();
    }

    /**
     * Отримує статистику кешу (для моніторингу)
     */
    public static String getCacheStats() {
        return String.format("Кешованих класів: %d, кешованих біндингів: %d",
                BINDING_CACHE.size(),
                BINDING_CACHE.values().stream().mapToInt(List::size).sum());
    }
}