package com.berdachuk.aichat.core.util;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] PROCESS_RANDOM = new byte[5];
    private static final AtomicInteger COUNTER = new AtomicInteger(RANDOM.nextInt() & 0x7fffffff);

    static {
        RANDOM.nextBytes(PROCESS_RANDOM);
    }

    private IdGenerator() {
    }

    public static String generateId() {
        byte[] bytes = new byte[12];
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        bytes[0] = (byte) (timestamp >>> 24);
        bytes[1] = (byte) (timestamp >>> 16);
        bytes[2] = (byte) (timestamp >>> 8);
        bytes[3] = (byte) timestamp;
        System.arraycopy(PROCESS_RANDOM, 0, bytes, 4, 5);
        int counter = COUNTER.incrementAndGet() & 0x00ffffff;
        bytes[9] = (byte) (counter >>> 16);
        bytes[10] = (byte) (counter >>> 8);
        bytes[11] = (byte) counter;
        return HexFormat.of().formatHex(bytes);
    }
}
