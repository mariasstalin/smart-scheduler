package com.smartscheduler.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

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

    public static Instant toInstantDate(String dateStr, ZoneId zoneId) {
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

}
