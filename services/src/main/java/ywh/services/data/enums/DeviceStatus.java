package ywh.services.data.enums;

public enum DeviceStatus {
    WORKING("Очікує нові дані"),
    TRY_START("Запуск"),
    STOPPED("Зупинено"),
    ERROR("Помилка"),
    TRY_PARSE("Парсинг"),
    PARSING_ERROR("Помилка парсингу"),
    CONNECTING("Підключення..."),
    CONNECTED("Підключено"),
    RECONNECTING("Перепідключення..."),
    CONNECTION_LOST("З'єднання втрачено");

    ;

    private final String displayText;

    DeviceStatus(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }
}

