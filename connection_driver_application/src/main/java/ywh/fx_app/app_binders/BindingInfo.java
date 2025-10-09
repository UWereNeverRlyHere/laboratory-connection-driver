package ywh.fx_app.app_binders;

import lombok.Data;

import java.lang.reflect.Field;
@Data
public class BindingInfo {
    public final Field field;
    public final SettingsBind annotation;
    public final PropertyBinder binder;
    public final Class<?> targetModel;

    public BindingInfo(Field field, SettingsBind annotation, PropertyBinder binder, Class<?> targetModel) {
        this.field = field;
        this.annotation = annotation;
        this.binder = binder;
        this.targetModel = targetModel;
    }
}
