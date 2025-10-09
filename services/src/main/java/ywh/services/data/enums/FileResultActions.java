package ywh.services.data.enums;


public enum FileResultActions {
    PRINT, SAVE_PDF, SAVE_DOCX, SEND, API, CREATE_DBF_FILE;

    @Override
    public String toString() {
        return switch (this){
            case PRINT -> "Друк";
            case SAVE_PDF -> "Зберігати копію .pdf";
            case SAVE_DOCX -> "Зберігати копію .docx";
            case SEND -> "Відправка на пошту";
            case API -> "Інтеграція з АПІ";
            case CREATE_DBF_FILE -> "Генерація файлу .dbf";
        };
    }
}
