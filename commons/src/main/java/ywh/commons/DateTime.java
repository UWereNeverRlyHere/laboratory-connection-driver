package ywh.commons;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

public class DateTime {
    private static final String ASTM_PATTERN = "yyyyMMddHHmmss";

    private DateTime() {
    }

    public static String getTime(String pattern) {
        return getDate(pattern);
    }

    public static String getDate(String pattern) {
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat(pattern);
        return formatForDateNow.format(dateNow);
    }

    public static String getDateTimeForJson() {
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return formatForDateNow.format(dateNow);
    }


    public static String getHl7DateTime() {
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatForDateNow.format(dateNow);
    }

    public static String getDateTime() {
        return getDate("dd.MM.yyyy HH:mm:ss ");
    }

    public static String getAstmMinusDateTime() {
        Date dateNow = new Date();
        dateNow.setTime(System.currentTimeMillis() - (604800000L *4));
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatForDateNow.format(dateNow);
    }

    private static String getAstmDateTime(int hourOffset) {
        return LocalDateTime.now()
                .plusHours(hourOffset)
                .format(DateTimeFormatter.ofPattern(ASTM_PATTERN));
    }

    /** Текущее время без смещения (оставлено для совместимости). */
    public static String getASTMDateTime() {
        return getAstmDateTime(0);
    }
    public static String getDateTimeForLog() {
        return "\r\n" + DateTime.getDate("dd-MM-yyyy") + " " + DateTime.getTime("HH:mm:ss:SSS");
    }

    public static String getDateTimeForTable() {
        return   DateTime.getDate("dd-MM-yyyy") + " " + DateTime.getTime("HH:mm:ss");
    }

    public static String tryGetFromPatternOrCurrent(String inputPattern, String inputDateTime) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputPattern);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(inputDateTime, inputFormatter);
            return dateTime.format(outputFormatter);
        }catch (Exception e) {
            return DateTime.getDateTimeForJson();
        }
    }

    public static String toPattern(String inputPattern,String outputPattern, String inputDateTime) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputPattern);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputPattern);
            LocalDateTime dateTime = LocalDateTime.parse(inputDateTime, inputFormatter);
            return dateTime.format(outputFormatter);
        }catch (Exception e) {
            Date dateNow = new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat(outputPattern);
            return formatForDateNow.format(dateNow);
        }
    }

    public static String tryGetFromPatternOrCurrent(String inputPattern, String inputDateTime, Locale locale) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputPattern,locale);
            TemporalAccessor parsed = inputFormatter.parseBest(
                    inputDateTime,
                    LocalDateTime::from,
                    LocalDate::from);

            LocalDateTime dateTime;
            if (parsed instanceof LocalDateTime) {
                dateTime = (LocalDateTime) parsed;
            } else {
                LocalDate date = (LocalDate) parsed;
                dateTime = date.atTime(LocalTime.now());
            }
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return dateTime.format(outputFormatter);
        } catch (Exception ex) {
            return getDateTimeForJson();
        }
    }

    public static String tryGetFromASTMOrCurrentV2(String dateTime) {
        return tryGetFromPatternOrCurrent("yyyyMMddHHmmss", dateTime);
    }

    /**
     * Метод конвертує дату АСТМ та ШЛ7 У дату для відправки
     * @param dateTime стрінгове значення дати від аналізатора, протоколів АСТМ та ШЛ7
     * @return Значення дати для тіла запиту, для відправки у МІС
     */
    public static String tryGetFromASTMOrCurrentV1(String dateTime) {
        try {
            String year, day, month, hour, min, sec;
            year = dateTime.substring(0, 4);
            month = dateTime.substring(4, 6);
            day = dateTime.substring(6, 8);
            hour = dateTime.substring(8, 10);
            min = dateTime.substring(10, 12);
            sec = "00";
            if (dateTime.length() >= 14) {
                sec = dateTime.substring(12, 14);
            }
            return year + "-" + month + "-" + day + "T" + hour + ":" + min + ":" + sec;
        } catch (Exception ex) {
            return DateTime.getDateTimeForJson();
        }
    }


}
