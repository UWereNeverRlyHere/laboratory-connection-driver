package ywh.services.device.protocol.custom;

import ywh.services.data.enums.SpecialBytes;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Фабрика однократних правил термінації.
 * Після вибору конкретного метода отримуємо готову {@link ITerminationStrategy}.
 */
public final class TerminationFactory {

    private TerminationFactory() {}

    public static ITerminationStrategy byEndByte(byte endByte) {
        return (b, buf) -> b == endByte;
    }

    public static ITerminationStrategy byEndByte(SpecialBytes endByte) {
        return (b, buf) -> b == endByte.getValue();
    }

    public static ITerminationStrategy byEndString(String endString) {
        return byEndString(0, endString);
    }

    /**
     * Завершення кадру послідовністю байт {@code endString}.
     *
     * @param skipSize  к-ть байт, які дозволено «пропустити» перед початком порівняння.
     *                  Якщо 0 – буде pattern.length * 2 (як у SimpleStringLogic).
     * @param endString шуканий кінець повідомлення.
     */
    public static ITerminationStrategy byEndString(int skipSize, String endString) {

        final byte[] pattern = endString.getBytes();
        final int initSkip = (skipSize == 0 ? pattern.length * 2 : skipSize);
        final int[] skip = {initSkip};                 // mutable у лямбді

        return new ITerminationStrategy() {
            public boolean analyze(byte b, ByteArrayOutputStream buf) {
                byte[] arr = buf.toByteArray();

                if (arr.length >= skip[0] + pattern.length) {
                    for (int i = 0; i < pattern.length; i++) {
                        if (arr[skip[0] + i] != pattern[i]) {  // розбіжність
                            skip[0]++;                         // зсуваємо вікно
                            return false;
                        }
                    }
                    // збіг знайдено
                    skip[0] = initSkip;                       // готові до нового пошуку
                    return true;
                }
                return false;
            }

            @Override
            public void reset() {
                skip[0] = initSkip;                           // повернутися у вихідний стан
            }
        };
    }

    /**
     * Кадр завершується, коли певний байт {@code countByte} зустрівся {@code howMany} разів.
     */
    public static ITerminationStrategy byByteCount(byte countByte, int howMany) {
        if (howMany <= 0)
            throw new IllegalArgumentException("howMany must be > 0");

        final int[] counter = {0};

        return new ITerminationStrategy() {

            @Override
            public boolean analyze(byte b, ByteArrayOutputStream buf) {
                if (b == countByte) counter[0]++;
                if (counter[0] == howMany) {
                    counter[0] = 0;
                    return true;
                }
                return false;
            }

            @Override
            public void reset() {
                counter[0] = 0;
            }
        };
    }

    /**
     * Кадр завершується після того, як зустріли {@code endByte},
     * і пройшло ще {@code offset} байт.
     */
    public static ITerminationStrategy byEndByteWithOffset(byte endByte, int offset) {
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");

        final int[] counter = {0};
        final int[] endSize = {Integer.MAX_VALUE};

        return new ITerminationStrategy() {
            @Override
            public boolean analyze(byte b, ByteArrayOutputStream buf) {
                counter[0]++;
                if (b == endByte) endSize[0] = counter[0] + offset;
                return counter[0] >= endSize[0];
            }

            @Override
            public void reset() {
                counter[0] = 0;
                endSize[0] = Integer.MAX_VALUE;
            }
        };
    }

    /**
     * Кадр завершується, коли обсяг буфера досяг мінімального розміру.
     */
    public static ITerminationStrategy byMinSize(int minSize) {
        if (minSize <= 0) throw new IllegalArgumentException("minSize must be > 0");
        return new ITerminationStrategy() {
            @Override
            public boolean analyze(byte b, ByteArrayOutputStream buf) {
                return buf.size() >= minSize;
            }
        };
    }

    public static ITerminationStrategy byStringThenByte(int skipSize, String endString, byte endByte) {
        final byte[] pattern = endString.getBytes();
        final int initSkip = (skipSize == 0 ? pattern.length * 2 : skipSize);
        final int[] skip = {initSkip};
        final boolean[] found = {false};

        return new ITerminationStrategy() {
            @Override
            public boolean analyze(byte b, ByteArrayOutputStream buf) {

                if (!found[0] && buf.size() >= skip[0] + pattern.length) {
                    byte[] arr = buf.toByteArray();
                    found[0] = Arrays.equals(
                            Arrays.copyOfRange(arr, skip[0], skip[0] + pattern.length),
                            pattern);
                    if (!found[0]) skip[0]++;                     // зсуваємо вікно
                }

                if (found[0] && b == endByte) {                   // завершальний байт
                    found[0] = false;
                    skip[0] = initSkip;
                    return true;
                }
                return false;
            }

            @Override
            public void reset() {
                found[0] = false;
                skip[0] = initSkip;
            }
        };
    }

}