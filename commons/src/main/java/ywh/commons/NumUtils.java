package ywh.commons;

import java.util.Optional;
import java.util.function.DoubleConsumer;

public class NumUtils {
    private NumUtils() {
    }

    public static Optional<Double> parseDouble(String str) {
        try {
            return Optional.of(Double.parseDouble(str.replace(',', '.')));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static void parseDouble(String str, DoubleConsumer onSuccess, Runnable onFailure) {
        try {
            onSuccess.accept(Double.parseDouble(str.replace(',', '.')));
        } catch (NumberFormatException e) {
            onFailure.run();
        }
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isDouble(String digit) {
        try {
            Double.parseDouble(digit);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isDigit(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
}
