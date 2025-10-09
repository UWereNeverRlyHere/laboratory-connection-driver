package ywh.commons.data;

import lombok.Getter;

public enum ConsoleColor {
    RED("%red","\u001B[31m"),
    GREEN("%green","\u001B[32m"),
    YELLOW("%yellow","\u001B[33m"),
    BLUE("%blue","\u001B[34m"),
    MAGENTA("%magenta","\u001B[35m"),
    CYAN("%cyan","\u001B[36m"),
    WHITE("%white","\u001B[37m"),
    GRAY("%gray","\u001B[30m");
    @Getter
    private final String logJ4Color;
    @Getter
    private final String consoleColor;

    ConsoleColor(String logJ4Color, String consoleColor) {
        this.logJ4Color = logJ4Color;
        this.consoleColor = consoleColor;
    }

}
