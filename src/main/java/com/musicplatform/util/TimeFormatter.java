package com.musicplatform.util;

public final class TimeFormatter {

    private TimeFormatter() {
    }

    public static String formatDuration(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds < 0) {
            return "0:00";
        }
        int hours = durationSeconds / 3600;
        int minutes = (durationSeconds % 3600) / 60;
        int seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
