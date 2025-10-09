package ywh.fx_app.app_binders;

import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import lombok.Getter;
import org.controlsfx.control.CheckComboBox;
import ywh.fx_app.app_custom_nodes.ITextField;
import ywh.fx_app.app_exceptions.SettingsValidationException;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Перечисляет все стандартные биндеры для разных UI–полей.
 */
@Getter
public enum PropertyBinders {
    TEXT_FIELD(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object fieldValue = uiField.get(ctl);
                switch (fieldValue) {
                    case ITextField textField -> {
                        Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                        mf.setAccessible(true);
                        Object val = mf.get(model);
                        textField.setFieldText(val == null ? "" : val.toString());
                    }
                    case TextField tf -> {
                        Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                        mf.setAccessible(true);
                        Object val = mf.get(model);
                        tf.setText(val == null ? "" : val.toString());
                    }
                    default ->
                            throw new SettingsValidationException("Field is not TextField or ITextField: " + uiField.getName());
                }
            } catch (Exception e) {
                throw new SettingsValidationException("TEXT_FIELD.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object fieldValue = uiField.get(ctl);
                String text = switch (fieldValue) {
                    case ITextField textField -> textField.getFieldText();
                    case TextField tf -> tf.getText();
                    default ->
                            throw new SettingsValidationException("Field is not TextField or ITextField: " + uiField.getName());
                };
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Class<?> type = mf.getType();
                Object parsed = switch (type.getSimpleName()) {
                    case "int", "Integer" -> text.isEmpty() ? null : Integer.parseInt(text);
                    case "String" -> text;
                    case "File" -> text == null || text.isEmpty() ? null : new File(text);
                    default -> text;
                };
                mf.set(model, parsed);
            } catch (Exception e) {
                throw new SettingsValidationException("TEXT_FIELD.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object fieldValue = uiField.get(ctl);
                String uiValue = switch (fieldValue) {
                    case ITextField textField -> textField.getFieldText();
                    case TextField tf -> tf.getText();
                    default ->
                            throw new SettingsValidationException("Field is not TextField or ITextField: " + uiField.getName());
                };

                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Object modelValue = mf.get(model);
                String modelValueStr = modelValue == null ? "" : modelValue.toString();

                return !Objects.equals(uiValue, modelValueStr);
            } catch (Exception e) {
                throw new SettingsValidationException("TEXT_FIELD.hasChanges: " + e.getMessage(), e);
            }
        }
    }),

    MFX_COMBO_BOX(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object comboBox = uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Object val = mf.get(model);

                if (comboBox instanceof MFXFilterComboBox) {
                    @SuppressWarnings("unchecked")
                    MFXFilterComboBox<String> filterCombo = (MFXFilterComboBox<String>) comboBox;
                    filterCombo.setText(val == null ? "" : val.toString());

                } else if (comboBox instanceof MFXComboBox) {
                    @SuppressWarnings("unchecked")
                    MFXComboBox<Object> mfxCombo = (MFXComboBox<Object>) comboBox;

                    // Для MFXComboBox намагаємося встановити значення через селекцію
                    if (val != null) {
                        // Спочатку шукаємо точне співпадіння
                        if (mfxCombo.getItems().contains(val)) {
                            mfxCombo.getSelectionModel().selectItem(val);
                        } else {
                            // Якщо точного співпадіння немає, шукаємо за toString()
                            String valStr = val.toString();
                            for (Object item : mfxCombo.getItems()) {
                                if (item.toString().equals(valStr)) {
                                    mfxCombo.getSelectionModel().selectItem(item);
                                    break;
                                }
                            }
                        }
                    }

                } else if (comboBox instanceof ComboBox) {
                    // Фолбек для звичайних ComboBox
                    @SuppressWarnings("unchecked")
                    ComboBox<Object> standardCombo = (ComboBox<Object>) comboBox;
                    standardCombo.setValue(val);
                }

            } catch (Exception e) {
                throw new SettingsValidationException("MFX_COMBO_BOX.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object comboBox = uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Class<?> targetType = mf.getType();

                Object rawValue = null;

                if (comboBox instanceof MFXFilterComboBox) {
                    MFXFilterComboBox<?> filterCombo = (MFXFilterComboBox<?>) comboBox;
                    rawValue = filterCombo.getText();

                } else if (comboBox instanceof MFXComboBox) {
                    MFXComboBox<?> mfxCombo = (MFXComboBox<?>) comboBox;
                    rawValue = mfxCombo.getValue();

                } else if (comboBox instanceof ComboBox) {
                    ComboBox<?> standardCombo = (ComboBox<?>) comboBox;
                    rawValue = standardCombo.getValue();
                }

                // Конвертуємо значення до потрібного типу
                Object convertedValue = convertValue(rawValue, targetType);
                if (convertedValue != null)
                    mf.set(model, convertedValue);

            } catch (Exception e) {
                throw new SettingsValidationException("MFX_COMBO_BOX.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Object comboBox = uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);

                Object uiValue = null;

                if (comboBox instanceof MFXFilterComboBox) {
                    MFXFilterComboBox<?> filterCombo = (MFXFilterComboBox<?>) comboBox;
                    uiValue = filterCombo.getText();

                } else if (comboBox instanceof MFXComboBox) {
                    MFXComboBox<?> mfxCombo = (MFXComboBox<?>) comboBox;
                    uiValue = mfxCombo.getValue();

                } else if (comboBox instanceof ComboBox) {
                    ComboBox<?> standardCombo = (ComboBox<?>) comboBox;
                    uiValue = standardCombo.getValue();
                }

                Object modelValue = mf.get(model);

                // Конвертуємо UI значення до типу моделі для порівняння
                Object convertedUiValue = convertValue(uiValue, mf.getType());

                return !Objects.equals(convertedUiValue, modelValue);

            } catch (Exception e) {
                throw new SettingsValidationException("MFX_COMBO_BOX.hasChanges: " + e.getMessage(), e);
            }
        }

        /**
         * Допоміжний метод для конвертації значень
         */
        private Object convertValue(Object value, Class<?> targetType) {
            if (value == null) {
                return null;
            }

            String valueStr = value.toString().trim();
            if (valueStr.isEmpty()) {
                return null;
            }

            try {
                return switch (targetType.getSimpleName()) {
                    case "int", "Integer" -> Integer.parseInt(valueStr);
                    case "long", "Long" -> Long.parseLong(valueStr);
                    case "double", "Double" -> Double.parseDouble(valueStr);
                    case "float", "Float" -> Float.parseFloat(valueStr);
                    case "boolean", "Boolean" -> Boolean.parseBoolean(valueStr);
                    case "String" -> valueStr;
                    default -> {
                        // Якщо тип enum
                        if (targetType.isEnum()) {
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            Object enumValue = Enum.valueOf((Class<Enum>) targetType, valueStr);
                            yield enumValue;
                        }
                        // Інакше повертаємо як String
                        yield valueStr;
                    }
                };
            } catch (IllegalArgumentException e) {
                // Якщо конвертація не вдалася, повертаємо як String
                return valueStr;
            }
        }
    }),

    STRING_COMBO_BOX(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<String> cb = (ComboBox<String>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                String val = (String) mf.get(model);
                if (val != null)
                    cb.setValue(val);
            } catch (Exception e) {
                throw new SettingsValidationException("STRING_COMBO_BOX.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<String> cb = (ComboBox<String>) uiField.get(ctl);
                String val = cb.getValue();
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                mf.set(model, val);
            } catch (Exception e) {
                throw new SettingsValidationException("STRING_COMBO_BOX.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<String> cb = (ComboBox<String>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);

                String uiValue = cb.getValue();
                String modelValue = (String) mf.get(model);

                return !Objects.equals(uiValue, modelValue);
            } catch (Exception e) {
                throw new SettingsValidationException("STRING_COMBO_BOX.hasChanges: " + e.getMessage(), e);
            }
        }
    }),

    ENUM_COMBO_BOX(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<Object> cb = (ComboBox<Object>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Object val = mf.get(model);
                cb.setValue(val);
            } catch (Exception e) {
                throw new SettingsValidationException("ENUM_COMBO_BOX.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<Object> cb = (ComboBox<Object>) uiField.get(ctl);
                Object val = cb.getValue();
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                mf.set(model, val);
            } catch (Exception e) {
                throw new SettingsValidationException("ENUM_COMBO_BOX.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<Object> cb = (ComboBox<Object>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);

                Object uiValue = cb.getValue();
                Object modelValue = mf.get(model);

                return !Objects.equals(uiValue, modelValue);
            } catch (Exception e) {
                throw new SettingsValidationException("ENUM_COMBO_BOX.hasChanges: " + e.getMessage(), e);
            }
        }
    }),

    CHECK_COMBO_BOX(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                CheckComboBox<Object> ccb = (CheckComboBox<Object>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) mf.get(model);
                ccb.getCheckModel().clearChecks();
                if (list != null) {
                    list.forEach(ccb.getCheckModel()::check);
                }
            } catch (Exception e) {
                throw new SettingsValidationException("CHECK_COMBO_BOX.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                CheckComboBox<Object> ccb = (CheckComboBox<Object>) uiField.get(ctl);
                List<Object> selected = new ArrayList<>(ccb.getCheckModel().getCheckedItems());
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                mf.set(model, selected);
            } catch (Exception e) {
                throw new SettingsValidationException("CHECK_COMBO_BOX.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                @SuppressWarnings("unchecked")
                CheckComboBox<Object> ccb = (CheckComboBox<Object>) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);

                List<Object> uiSelected = new ArrayList<>(ccb.getCheckModel().getCheckedItems());
                @SuppressWarnings("unchecked")
                List<Object> modelSelected = (List<Object>) mf.get(model);

                // Порівнюємо списки (враховуємо null та порядок)
                if (modelSelected == null) {
                    return !uiSelected.isEmpty();
                }

                // Порівнюємо розміри та елементи (не залежить від порядку)
                if (uiSelected.size() != modelSelected.size()) {
                    return true;
                }

                return !uiSelected.containsAll(modelSelected) || !modelSelected.containsAll(uiSelected);
            } catch (Exception e) {
                throw new SettingsValidationException("CHECK_COMBO_BOX.hasChanges: " + e.getMessage(), e);
            }
        }
    }),

    CHECKBOX(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                CheckBox checkbox = (CheckBox) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Object val = mf.get(model);

                // Обробляємо як Boolean (об'єкт), так і boolean (примітив)
                boolean boolVal = false;
                if (val instanceof Boolean) {
                    boolVal = (Boolean) val;
                } else if (val != null) {
                    boolVal = (boolean) val;
                }

                checkbox.setSelected(boolVal);
            } catch (Exception e) {
                throw new SettingsValidationException("CHECKBOX.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                CheckBox checkbox = (CheckBox) uiField.get(ctl);
                boolean val = checkbox.isSelected();

                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);

                // Перевіряємо тип поля в моделі та встановлюємо відповідне значення
                Class<?> fieldType = mf.getType();
                if (fieldType == boolean.class) {
                    mf.setBoolean(model, val);
                } else if (fieldType == Boolean.class) {
                    mf.set(model, val);
                } else {
                    mf.set(model, val);
                }
            } catch (Exception e) {
                throw new SettingsValidationException("CHECKBOX.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                MFXCheckbox checkbox = (MFXCheckbox) uiField.get(ctl);
                boolean uiValue = checkbox.isSelected();

                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                Object modelVal = mf.get(model);

                boolean modelValue = false;
                if (modelVal instanceof Boolean) {
                    modelValue = (Boolean) modelVal;
                } else if (modelVal != null) {
                    modelValue = (boolean) modelVal;
                }

                return uiValue != modelValue;
            } catch (Exception e) {
                throw new SettingsValidationException("MFX_CHECKBOX.hasChanges: " + e.getMessage(), e);
            }
        }
    }),

    BOOLEAN_FLAG(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                boolean val = (boolean) mf.get(model);
                uiField.setAccessible(true);
                uiField.set(ctl, val);
            } catch (Exception e) {
                throw new SettingsValidationException("BOOLEAN_FLAG.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                uiField.setAccessible(true);
                boolean val = (boolean) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                mf.set(model, val);
            } catch (Exception e) {
                throw new SettingsValidationException("BOOLEAN_FLAG.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann)
                throws SettingsValidationException {
            try {
                uiField.setAccessible(true);
                boolean uiValue = (boolean) uiField.get(ctl);

                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                boolean modelValue = (boolean) mf.get(model);

                return uiValue != modelValue;
            } catch (Exception e) {
                throw new SettingsValidationException("BOOLEAN_FLAG.hasChanges: " + e.getMessage(), e);
            }
        }
    }),
    CHANGEABLE_BUTTON(new PropertyBinder() {
        @Override
        public void fill(Object ctl, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException {
            try {
                ywh.fx_app.app_custom_nodes.ChangeableButton btn =
                        (ywh.fx_app.app_custom_nodes.ChangeableButton) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                boolean modelState = (boolean) mf.get(model);

                if (btn.isState() != modelState) {
                    // Встановлюємо стан без зайвих ефектів — сам оновить іконку
                    btn.setState(modelState);
                }
            } catch (Exception e) {
                throw new SettingsValidationException("CHANGEABLE_BUTTON.fill: " + e.getMessage(), e);
            }
        }

        @Override
        public void commit(Object ctl, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException {
            try {
                ywh.fx_app.app_custom_nodes.ChangeableButton btn =
                        (ywh.fx_app.app_custom_nodes.ChangeableButton) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                mf.set(model, btn.isState());
            } catch (Exception e) {
                throw new SettingsValidationException("CHANGEABLE_BUTTON.commit: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasChanges(Object ctl, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException {
            try {
                ywh.fx_app.app_custom_nodes.ChangeableButton btn =
                        (ywh.fx_app.app_custom_nodes.ChangeableButton) uiField.get(ctl);
                Field mf = ann.targetModel().getDeclaredField(ann.targetField());
                mf.setAccessible(true);
                boolean modelState = (boolean) mf.get(model);
                return btn.isState() != modelState;
            } catch (Exception e) {
                throw new SettingsValidationException("CHANGEABLE_BUTTON.hasChanges: " + e.getMessage(), e);
            }
        }
    }),


    ;

    private final PropertyBinder binder;

    PropertyBinders(PropertyBinder binder) {
        this.binder = binder;
    }
}