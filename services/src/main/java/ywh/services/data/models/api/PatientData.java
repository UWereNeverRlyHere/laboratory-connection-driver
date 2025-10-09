package ywh.services.data.models.api;

import lombok.Getter;
import lombok.Setter;
import ywh.commons.ConvertUtil;
import ywh.commons.TextUtils;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PatientData {
    @Setter
    private String fullName = "";
    @Setter
    @Getter
    private String gender = "";
    @Setter
    @Getter
    private String birthday = "";
    @Setter
    private String fullNameTranslit = "";


    public PatientData(String fullName, String gender, String birthday) {
        this.fullName = fullName;
        this.gender = gender;
        this.birthday = birthday;
    }

    public String getFullName() {
        if (TextUtils.isNullOrEmpty(fullName)) return "";
        return fullName;
    }

    public String getFullNameWithoutSymbols() {
        return fullName.replaceAll("\\p{Punct}", "");
    }

    public String getFullNameTransliterate() {
        if (TextUtils.isNotNullOrEmpty(fullNameTranslit)) return fullNameTranslit;
        return ConvertUtil.toUaTransliterate(fullName);
    }

    public String getFullNameTransliterate(int maxLen) {
        if (TextUtils.isNotNullOrEmpty(fullNameTranslit))
            return fullNameTranslit.substring(0, Math.min(fullNameTranslit.length(), maxLen));
        if (TextUtils.isNullOrEmpty(fullName)) return "";
        String transliterate = ConvertUtil.toUaTransliterate(fullName);
        return transliterate.substring(0, Math.min(transliterate.length(), maxLen));

    }

    public String getFullName(int maxLen) {
        if (TextUtils.isNullOrEmpty(fullName)) return "";
        return fullName.substring(0, Math.min(fullName.length(), maxLen));
    }

    public String getBirthDayInFormat(String format) {
        if (TextUtils.isNullOrEmpty(birthday)) return "";
        LocalDateTime dateTime = LocalDateTime.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return dateTime.format(DateTimeFormatter.ofPattern(format));
    }


    public String getYears() {
        Period period = getPeriod();
        if (period == null) return "";
        return String.valueOf(period.getYears());
    }

    public String getYears(int minLength, char padChar) {
        Period period = getPeriod();
        if (period == null) return getInCorrectLength("", minLength, padChar);

        String yearsStr = String.valueOf(period.getYears());
        return getInCorrectLength(yearsStr, minLength, padChar);
    }

    public String getMonths() {
        Period period = getPeriod();
        if (period == null) return "";
        return String.valueOf(period.getMonths());
    }

    public String getMonths(int minLength, char padChar) {
        Period period = getPeriod();
        if (period == null) return getInCorrectLength("", minLength, padChar);
        String yearsStr = String.valueOf(period.getMonths());
        return getInCorrectLength(yearsStr, minLength, padChar);
    }

    private Period getPeriod() {
        try {
            if (TextUtils.isNullOrEmpty(birthday)) {
                return null;
            }
            LocalDateTime dateTime = LocalDateTime.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            // Перевіряємо, що дата народження не в майбутньому
            if (dateTime.isAfter(now)) {
                return null;
            }
            // Обчислюємо різницю в роках
            return Period.between(dateTime.toLocalDate(), now.toLocalDate());
        } catch (Exception e) {
            return null;
        }

    }

    private String getInCorrectLength(String string, int minLength, char padChar) {
        return string.length() >= minLength
                ? string
                : String.valueOf(padChar).repeat(minLength - string.length()) + string;
    }


    public String getGenderByType(String male, String female, String unknown) {
        if (TextUtils.isNullOrEmpty(gender)) return unknown;
        return switch (gender) {
            case "Female", "Feminin", "Жіноча" -> female;
            case "Male", "Masculin", "Чоловіча" -> male;
            default -> unknown;
        };

    }

}
