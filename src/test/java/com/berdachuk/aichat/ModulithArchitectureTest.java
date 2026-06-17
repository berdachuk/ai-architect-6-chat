package com.berdachuk.aichat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(AiChatApplication.class).verify();
    }
}
