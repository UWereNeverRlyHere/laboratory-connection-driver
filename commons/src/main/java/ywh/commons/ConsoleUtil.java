package ywh.commons;

import ywh.commons.data.ConsoleColor;
import static ywh.commons.data.ConsoleColor.*;

public class ConsoleUtil {
    private static final String RESET  = "\u001B[0m";

    private ConsoleUtil() {
    }
    public static void print(ConsoleColor color, String message) {
        System.out.println(color.getConsoleColor() + message + RESET);
    }

    public static void printRed(String message) {
        System.out.println(RED.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void printGreen(String message) {
        System.out.println(GREEN.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void printYellow(String message) {
        System.out.println(YELLOW.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void printBlue(String message) {
        System.out.println(BLUE.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void printMagenta(String message) {
        System.out.println(MAGENTA.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void printCyan(String message) {
        System.out.println(CYAN.getConsoleColor() + DateTime.getDateTime() + message + RESET);
    }

    public static void print(String message) {
        System.out.println(DateTime.getDateTime() + message);
    }
}
