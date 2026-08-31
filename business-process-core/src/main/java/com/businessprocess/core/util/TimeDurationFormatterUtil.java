package com.businessprocess.core.util;

import java.time.Duration;

public final class TimeDurationFormatterUtil {

    public static String getFormattedDuration(Duration duration) {
        if (duration == null) {
            duration = Duration.ZERO;
        }
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        StringBuilder formatted = new StringBuilder();

        if (days > 0) {
            formatted.append(days).append(days == 1 ? " day" : " days");
            if (hours > 0 || minutes > 0 || seconds > 0 || millis > 0) formatted.append(", ");
        }

        if (hours > 0) {
            formatted.append(hours).append(hours == 1 ? " hour" : " hours");
            if (minutes > 0 || seconds > 0 || millis > 0) formatted.append(", ");
        }

        if (minutes > 0) {
            formatted.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            if (seconds > 0 || millis > 0) formatted.append(", ");
        }

        if (seconds > 0) {
            formatted.append(seconds).append(seconds == 1 ? " second" : " seconds");
            if (millis > 0) formatted.append(", ");
        }

        if (millis > 0 || formatted.length() == 0) {
            formatted.append(millis).append(millis == 1 ? " millisecond" : " milliseconds");
        }

        return formatted.toString();
    }

    public static String getFormattedDuration(long durationMs) {
        return getFormattedDuration(Duration.ofMillis(durationMs));
    }
}
