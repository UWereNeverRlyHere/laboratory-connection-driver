package ywh.commons;

import java.util.Collection;

public class ConvertUtil {
    private ConvertUtil() {
    }

    public static byte[] hexToByteArray(String s) {
        s = s.replace(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String hexToString(String hex) {
        hex = hex.replace(" ", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String output = hex.substring(i, Math.min(i + 2, hex.length()));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char) decimal);
        }

        return sb.toString().replace("\r", "\n");
    }
    public static String toHex(byte[] byteBuffer) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte aByte : byteBuffer) {
            hexBuilder.append(String.format("%02X ", aByte));
        }
        return hexBuilder.toString();

    }

    public static String toHex(Collection<Byte> byteBuffer) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte aByte : byteBuffer) {
            hexBuilder.append(String.format("%02X ", aByte));
        }
        return hexBuilder.toString();
    }
    public static String toEnglishTransliterate(String message) {
        try {
            // Кириличні символи (російські та українські)
            char[] abcCyr = {' ', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'і', 'ї', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'І', 'Ї', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я'};

            // Відповідні латинські символи
            String[] abcLat = {" ", "a", "b", "v", "g", "d", "e", "yo", "zh", "z", "i", "i", "yi", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya", "A", "B", "V", "G", "D", "E", "Yo", "Zh", "Z", "I", "Y", "I", "Yi", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "F", "Kh", "Ts", "Ch", "Sh", "Shch", "", "Y", "", "E", "Yu", "Ya"};

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < message.length(); i++) {
                char currentChar = message.charAt(i);
                boolean found = false;

                // Шукаємо кириличний символ у масиві
                for (int x = 0; x < abcCyr.length; x++) {
                    if (currentChar == abcCyr[x]) {
                        builder.append(abcLat[x]);
                        found = true;
                        break;
                    }
                }

                // Якщо символ не знайдено (це може бути цифра, розділовий знак, тощо), додаємо як є
                if (!found) {
                    builder.append(currentChar);
                }
            }

            return builder.toString();
        } catch (Exception ex) {
            return message;
        }
    }



    public static String toUaTransliterate(String message) {
        try {
            char[] abcCyr = {' ', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'і', 'ї', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'І', 'Ї', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
            String[] abcLat = {" ", "a", "b", "v", "g", "d", "e", "e", "zh", "z", "u", "i", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "", "u", "", "e", "ju", "ja", "A", "B", "V", "G", "D", "E", "E", "Zh", "Z", "U", "Y", "I", "I", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "F", "H", "Ts", "Ch", "Sh", "Sch", "", "U", "", "E", "Ju", "Ja", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < message.length(); i++) {
                for (int x = 0; x < abcCyr.length; x++) {
                    if (message.charAt(i) == abcCyr[x]) {
                        builder.append(abcLat[x]);
                    }
                }
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }

}