package ywh.fx_app.app_managers;

import ywh.commons.ThrowingRunnable;

import java.util.HashMap;
import java.util.Map;

public class CheckManager {
    private final Map<String, ThrowingRunnable> items = new HashMap<>();

    public CheckManager add(String key, ThrowingRunnable checker) {
        items.putIfAbsent(key,checker);
        return this;
    }
    public void check() throws Exception {
        for (ThrowingRunnable checker : items.values()) {
            checker.run();
        }
    }

    public void clear() {
        items.clear();
    }
}
