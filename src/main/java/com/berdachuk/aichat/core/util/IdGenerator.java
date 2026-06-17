package com.berdachuk.aichat.core.util;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String generateId() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        bytes[0] = (byte) (timestamp >>> 24);
        bytes[1] = (byte) (timestamp >>> 16);
        bytes[2] = (byte) (timestamp >>> 8);
        bytes[3] = (byte) timestamp;
        return HexFormat.of().formatHex(bytes);
    }
}
