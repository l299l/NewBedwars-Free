package com.l299l.newbedwars.utils;

import com.l299l.newbedwars.config.properties.Properties;

import java.time.LocalDateTime;

public class TimeUtils {
    public static String formatTime(int time) {
        return formatTime(time, Boolean.TRUE.equals(Properties.ClockTimeFormat));
    }

    public static String formatTime(int time, boolean clock) {
        if (clock) {
            int total = Math.max(0, time);
            return format(total / 60) + ":" + format(total % 60);
        }
        if (time < 0) {
            return "0m 0s";
        }else if (time < 60) {
            return time + "s";
        }
        int minutes = time / 60;
        int seconds = time % 60;
        return minutes + "m " + seconds + "s";
    }

    public static String getActualDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        if (year > 2000) {
            year -= 2000;
        }
        return format(now.getMonthValue()) + "/" + format(now.getDayOfMonth()) + "/" + year + " " + format(now.getHour()) + ":" + format(now.getMinute()) + ":" + format(now.getSecond());
    }

    public static String getActualDate() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        if (year > 2000) {
            year -= 2000;
        }
        return now.getMonthValue() + "/" + now.getDayOfMonth() + "/" + year;
    }

    private static String format(int time) {
        if (time < 10) {
            return "0" + time;
        }
        return String.valueOf(time);
    }
}
