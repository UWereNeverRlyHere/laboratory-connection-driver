package ywh.services.settings;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import ywh.services.device.parsers.IParser;

public class ParserExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return IParser.class.isAssignableFrom(f.getDeclaredClass());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
