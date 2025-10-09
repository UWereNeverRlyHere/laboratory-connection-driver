package ywh.commons;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class TextUtils {
    private TextUtils() {}

    public static class StringChecker {
        private final String value;
        @Getter
        private final boolean isNullOrEmpty;

        private StringChecker(String value) {
            this.value = value;
            this.isNullOrEmpty = value == null || value.isEmpty();
        }

        public String getIfNullOrEmpty(String defaultValue) {
            return isNullOrEmpty ? defaultValue : value;
        }

        public String getIfNullOrEmpty() {
            return isNullOrEmpty ? "" : value;
        }


        public String getIfNotNullOrEmpty(String defaultValue) {
            return !isNullOrEmpty ? defaultValue : value;
        }

        public String getIfNotNullOrEmpty() {
            return !isNullOrEmpty ? value : "";
        }


        public StringChecker ifNullOrEmpty(Runnable action) {
            if (isNullOrEmpty) action.run();
            return this;
        }

        public StringChecker ifNotNullOrEmpty(Runnable action) {
            if (!isNullOrEmpty) action.run();
            return this;
        }
        public StringChecker ifNullOrEmptyThrow(String errorMessage) {
            if (isNullOrEmpty) throw new IllegalArgumentException(errorMessage);
            return this;
        }

        public <T extends Exception> StringChecker ifNullOrEmptyThrow(T exception) throws T {
            if (isNullOrEmpty) throw exception;
            return this;
        }
        public StringChecker ifNotNullOrEmptyThrow(String errorMessage) {
            if (!isNullOrEmpty) throw new IllegalArgumentException(errorMessage);
            return this;
        }

        public <T extends Exception> StringChecker ifNotNullOrEmptyThrow(T exception) throws T {
            if (!isNullOrEmpty) throw exception;
            return this;
        }

        public String get() {
            return value;
        }


        public boolean isNotNullOrEmpty() {
            return !isNullOrEmpty;
        }
    }



    public static StringChecker check(String string) {
        return new StringChecker(string);
    }


    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
    public static boolean isNullOrEmpty(String... strings) {
        return strings != null && Arrays.stream(strings).allMatch(s -> s == null || s.isEmpty());
    }

    public static boolean isNotNullOrEmpty(String string) {
        return !isNullOrEmpty(string);
    }
    public static boolean isNotNullOrEmpty(String ...strings) {
        return !isNullOrEmpty(strings);
    }


    public static String getIfNullOrEmpty(String string, String defaultValue) {
        return isNullOrEmpty(string) ? defaultValue : string;
    }


    public static String getIfNotNullOrEmpty(String string, String defaultValue) {
        return isNotNullOrEmpty(string) ? defaultValue : string;
    }

    public static String getValueWithoutZeroOrDotAtStart(String value) {
        if (value.startsWith(".")) return "0" + value;

        if (!value.startsWith("0."))
            return value.replaceFirst("^0(?!$)", "");

        return value;
    }

    public static String getStringFromArrayByPlace(String[] array, int place) {
        if (array.length > place) return array[place];
        return "";
    }

    public static String[] getEmptyArray(int length) {
        String[] empty = new String[length];
        Arrays.fill(empty, "");
        return empty;
    }

    public static String getAfterDot(String part) {
        return part.substring(part.indexOf(".") + 1);
    }

    public static String getAfterColon(String part) {
        return part.substring(part.indexOf(":") + 1);
    }
    public static String toString(byte [] bytes, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
    }
}
