package ywh.fx_app.app_managers;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;

import java.util.HashMap;
import java.util.Map;

public class DisableStateManager {
    private final Map<String, BooleanProperty> conditions = new HashMap<>();
    private BooleanBinding disableBinding;

    public DisableStateManager() {
        updateBinding();
    }

    // 🔧 Додає нову умову
    public DisableStateManager addCondition(String name, boolean initialValue) {
        conditions.put(name, new SimpleBooleanProperty(initialValue));
        updateBinding();
        return this;
    }

    // 🔧 Встановлює стан умови
    public DisableStateManager setCondition(String name, boolean value) {
        BooleanProperty property = conditions.get(name);
        if (property != null) {
            property.set(value);
        }
        return this;
    }

    // 🔧 Видаляє умову
    public DisableStateManager removeCondition(String name) {
        conditions.remove(name);
        updateBinding();
        return this;
    }

    // 🔧 Прив'язує до контрола
    public void bindTo(Control control) {
        control.disableProperty().bind(disableBinding);
    }

    private void updateBinding() {
        if (conditions.isEmpty()) {
            disableBinding = Bindings.createBooleanBinding(() -> false);
        } else {
            BooleanProperty[] properties = conditions.values().toArray(new BooleanProperty[0]);
            disableBinding = Bindings.createBooleanBinding(
                    () -> conditions.values().stream().anyMatch(BooleanProperty::get),
                    properties
            );
        }
    }

    // 🔧 Отримує поточний стан
    public boolean isDisabled() {
        return disableBinding.get();
    }
}
