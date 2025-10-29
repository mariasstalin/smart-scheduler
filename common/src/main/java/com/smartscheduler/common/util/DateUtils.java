package com.smartscheduler.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtils {

    private static final DateTimeFormatter RASA_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter ZOHO_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    private static final ZoneId DEFAULT_API_ZONE = ZoneId.of("UTC");

    private DateUtils() {
        // Utility class
    }

    public static Instant toInstant(LocalDate localDate, ZoneId zoneId) {
        return localDate.atStartOfDay(zoneId).toInstant();
    }

    public static Instant toInstantUTC(LocalDate localDate) {
        return toInstant(localDate, ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(Instant instant, ZoneId zoneId) {
        return instant.atZone(zoneId).toLocalDate();
    }

    public static LocalDate toLocalDateUTC(Instant instant) {
        return toLocalDate(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId).toInstant();
    }

    public static Instant toInstantUTC(LocalDateTime localDateTime) {
        return toInstant(localDateTime, ZoneOffset.UTC);
    }

    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant toInstantDate(String dateStr, String pattern, ZoneId zoneId) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        return localDate.atStartOfDay(zoneId).toInstant();
    }

    public static Instant parseDateTimeIso(String isoTime) {
        if (isoTime == null || isoTime.isBlank()) throw new IllegalArgumentException("No valid date provided");
        try {
            return Instant.parse(isoTime.replace("+00:00", "Z"));
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse date: " + isoTime, e);
        }
    }

    public static Instant parseZohoDateTime(String dateTimeString, ZoneId zoneId) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            throw new IllegalArgumentException("Date time string cannot be null or empty.");
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateTimeString, RASA_DATE_TIME_FORMATTER);
            ZonedDateTime zdt = ldt.atZone(zoneId);
            return zdt.toInstant();
        } catch (DateTimeParseException e) {
            //log.error("Failed to parse date string: {} using format {}", dateTimeString, RASA_DATE_TIME_FORMATTER.toString(), e);
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD HH:MM.", e);
        }
    }

    public static String toFormattedDateTimeString(LocalDateTime localDateTime, ZoneId zoneId) {
        ZonedDateTime utcZonedTime = localDateTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime convertedTime = utcZonedTime.withZoneSameInstant(zoneId);
        return convertedTime.format(RASA_DATE_TIME_FORMATTER);
    }

    public static Instant toInstantDate(String dateStr, ZoneId zoneId) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr, ZOHO_DATE_FORMATTER);
            return localDate.atStartOfDay(zoneId).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format.", e);
        }
    }

}
