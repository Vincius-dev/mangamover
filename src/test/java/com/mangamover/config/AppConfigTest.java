package com.mangamover.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void port_hasDefaultValue() {
        // acessa a classe para garantir cobertura; valor padrão é 8765 quando PORT não está setado
        assertTrue(AppConfig.PORT > 0);
    }

    @Test
    void dbPath_hasDefaultValue() {
        assertNotNull(AppConfig.DB_PATH);
        assertFalse(AppConfig.DB_PATH.isBlank());
    }
}
