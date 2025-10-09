package ywh.fx_app.app_binders;

import ywh.fx_app.app_exceptions.SettingsValidationException;

import java.lang.reflect.Field;

public interface PropertyBinder {
    void fill(Object controller, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException;
    void commit(Object controller, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException;
    boolean hasChanges(Object controller, Object model, Field uiField, SettingsBind ann) throws SettingsValidationException;

}
