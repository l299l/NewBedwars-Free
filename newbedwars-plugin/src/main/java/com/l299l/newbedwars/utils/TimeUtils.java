package com.l299l.newbedwars.utils;

import java.time.LocalDateTime;

public class TimeUtils {
    public static String formatTime(int time) {
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
