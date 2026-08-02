package com.dbtraining.tradeflow.model;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconResultTest {

    @Test
    void detectedAtUsesCreatedAtColumnForCompatibility() throws NoSuchFieldException {
        Field field = ReconResult.class.getDeclaredField("detectedAt");
        Column column = field.getAnnotation(Column.class);

        assertEquals("created_at", column.name());
    }
}
