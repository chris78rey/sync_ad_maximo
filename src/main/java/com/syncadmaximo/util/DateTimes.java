package com.syncadmaximo.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimes {

    private static final DateTimeFormatter DATE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateTimes() {
    }

    public static Instant now(Clock clock) {
        return Instant.now(clock);
    }

    public static LocalDate today(ZoneId zoneId) {
        return LocalDate.now(zoneId);
    }

    public static String formatDate(LocalDate date) {
        return DATE_YYYYMMDD.format(date);
    }

    public static String formatRunId(LocalDateTime dateTime) {
        return DATETIME_BASIC.format(dateTime);
    }

    public static String formatTimestamp(Instant instant, ZoneId zoneId) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zoneId));
    }
}
