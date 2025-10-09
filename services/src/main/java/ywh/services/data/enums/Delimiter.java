package ywh.services.data.enums;

public enum Delimiter {
    DOT("."),
    COMA(",");

    private final String message;

    Delimiter(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

    public String getReplacement() {
        return switch (this) {
            case DOT -> COMA.toString();
            case COMA -> "\\" + DOT;
        };

    }

}
