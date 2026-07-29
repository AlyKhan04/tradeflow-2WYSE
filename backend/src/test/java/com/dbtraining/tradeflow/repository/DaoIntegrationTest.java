package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Counterparty;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for TICKET-I046 (ReconResultDAO) and TICKET-I047
 * (CounterpartyDAO), running against the real Liquibase-migrated H2 schema
 * and the Day-1 seed data.
 */
@SpringBootTest
class DaoIntegrationTest {

    @Autowired
    private DataSource dataSource;

    private ReconResultDAO reconResultDAO;
    private CounterpartyDAO counterpartyDAO;

    @BeforeEach
    void setUp() {
        reconResultDAO = new ReconResultDAO(dataSource);
        counterpartyDAO = new CounterpartyDAO(dataSource);
    }

    // ---------------------------------------------------------------- I047

    @Test
    void findAll_returnsSeededCounterpartiesOrderedByName() {
        List<Counterparty> all = counterpartyDAO.findAll();

        assertNotNull(all);
        assertFalse(all.isEmpty(), "Day-1 seed should provide counterparties");

        // ORDER BY name — verify the ordering contract actually holds.
        for (int i = 1; i < all.size(); i++) {
            String previous = all.get(i - 1).getName();
            String current = all.get(i).getName();
            assertTrue(previous.compareTo(current) <= 0,
                    "rows must arrive ordered by name, but '" + previous
                            + "' preceded '" + current + "'");
        }
    }

    @Test
    void findByRegion_validRegion_returnsOnlyThatRegion() {
        List<Counterparty> emea = counterpartyDAO.findByRegion("EMEA");

        assertNotNull(emea);
        emea.forEach(cp -> assertEquals("EMEA", cp.getRegion()));
    }

    @Test
    void findByRegion_invalidRegion_throwsBeforeHittingDatabase() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> counterpartyDAO.findByRegion("XYZ"));

        // The message must list the allowed values so the caller can self-correct.
        assertTrue(ex.getMessage().contains("APAC"), ex.getMessage());
        assertTrue(ex.getMessage().contains("EMEA"), ex.getMessage());
        assertTrue(ex.getMessage().contains("NAMR"), ex.getMessage());
        assertTrue(ex.getMessage().contains("LATAM"), ex.getMessage());
    }

    @Test
    void findByRegion_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> counterpartyDAO.findByRegion(null));
    }

    // ---------------------------------------------------------------- I046

    @Test
    void findByTradeId_noSuchTrade_returnsEmptyListNotNull() {
        List<ReconResult> results = reconResultDAO.findByTradeId(999_999L);

        assertNotNull(results, "must return an empty list, never null");
        assertTrue(results.isEmpty());
    }

    @Test
    void insert_thenFindUnresolved_returnsTheOpenBreak() {
        long tradeId = anyExistingTradeId();
        int before = reconResultDAO.findUnresolved().size();

        long newId = reconResultDAO.insert(ReconResult.builder()
                .tradeId(tradeId)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .status("OPEN")
                .build());

        assertTrue(newId > 0, "insert must return the generated key");

        List<ReconResult> unresolved = reconResultDAO.findUnresolved();
        assertEquals(before + 1, unresolved.size());
        unresolved.forEach(r -> assertEquals("OPEN", r.getStatus()));
        unresolved.forEach(r -> assertNotNull(r.getDetectedAt(), "detected_at must be populated"));
    }

    @Test
    void insert_thenFindByTradeId_findsTheBreakForThatTrade() {
        long tradeId = anyExistingTradeId();

        reconResultDAO.insert(ReconResult.builder()
                .tradeId(tradeId)
                .discrepancyType(DiscrepancyType.QUANTITY_MISMATCH)
                .status("OPEN")
                .build());

        List<ReconResult> forTrade = reconResultDAO.findByTradeId(tradeId);

        assertFalse(forTrade.isEmpty());
        forTrade.forEach(r -> assertEquals(tradeId, r.getTradeId()));
    }

    /** recon_breaks.trade_id is a foreign key, so inserts need a real trade. */
    private long anyExistingTradeId() {
        try (Connection cx = dataSource.getConnection();
             Statement st = cx.createStatement();
             ResultSet rs = st.executeQuery("SELECT MIN(id) FROM trades")) {
            assertTrue(rs.next(), "Day-1 seed should provide trades");
            return rs.getLong(1);
        } catch (Exception e) {
            throw new IllegalStateException("could not read a seeded trade id", e);
        }
    }
}
