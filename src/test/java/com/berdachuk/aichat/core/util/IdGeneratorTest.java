package com.berdachuk.aichat.core.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    void generateId_returns24CharHex() {
        String id = IdGenerator.generateId();
        assertThat(id).hasSize(24).matches("[0-9a-f]{24}");
    }

    @Test
    void generateId_isUniqueAcrossManyCalls() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(IdGenerator.generateId());
        }
        assertThat(ids).hasSize(10_000);
    }

    @Test
    void generateId_firstFourBytesEncodeCurrentTimestamp() {
        long before = System.currentTimeMillis() / 1000L;
        String id = IdGenerator.generateId();
        long after = System.currentTimeMillis() / 1000L;

        long embedded = Long.parseLong(id.substring(0, 8), 16);
        assertThat(embedded).isBetween(before, after);
    }
}
