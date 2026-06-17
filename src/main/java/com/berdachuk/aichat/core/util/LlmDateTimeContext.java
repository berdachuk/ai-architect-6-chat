package com.berdachuk.aichat.core.util;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class LlmDateTimeContext {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static volatile Clock clock = Clock.systemUTC();

    private LlmDateTimeContext() {
    }

    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    public static void resetClock() {
        clock = Clock.systemUTC();
    }

    public static String formatNowUtc() {
        return UTC_FORMATTER.format(ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String contextBlock() {
        return "Current date and time (UTC): " + formatNowUtc();
    }
}
