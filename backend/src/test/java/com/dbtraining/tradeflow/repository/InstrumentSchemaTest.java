package com.dbtraining.tradeflow.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
class InstrumentSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void instrumentsTable_containsIsinColumn() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getColumns(null, null, "INSTRUMENTS", "ISIN");
            assertTrue(rs.next(), "The instruments table must expose an ISIN column for the JPA entity mapping");
        }
    }
}
