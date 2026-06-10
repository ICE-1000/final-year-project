package com.inventory.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtils() {
    }

    public static String display(LocalDateTime value) {
        return value == null ? "" : DISPLAY_FORMAT.format(value);
    }
}
