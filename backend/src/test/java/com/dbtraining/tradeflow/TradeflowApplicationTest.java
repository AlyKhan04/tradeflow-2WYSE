package com.dbtraining.tradeflow;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeflowApplicationTest {

    @Test
    void loadDemoCsvFile_createsTempFileFromClasspathResource() throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        Path tempFile = TradeflowApplication.loadDemoCsvFile(resourceLoader, "internal-trades.csv");

        assertTrue(Files.exists(tempFile));
        assertTrue(Files.size(tempFile) > 0);
        assertTrue(Files.readString(tempFile).contains("asset_class,trade_ref"));

        Files.deleteIfExists(tempFile);
    }
}
