package ywh.fx_app.app_binders;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SettingsBind {
    Class<?> targetModel();
    String   targetField();
    PropertyBinders binder() default PropertyBinders.TEXT_FIELD;
}
