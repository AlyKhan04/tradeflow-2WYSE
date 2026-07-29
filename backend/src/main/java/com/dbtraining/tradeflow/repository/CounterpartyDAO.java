package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.exception.JdbcException;
import com.dbtraining.tradeflow.model.Counterparty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ============================================================================
 * CounterpartyDAO — TICKET-I047 (Day 4)
 * ============================================================================
 * WHAT:    JDBC DAO for the counterparties table.
 * HOW:     PreparedStatement + try-with-resources, same recipe as the other DAOs.
 * WHY:     Day 6's region filter on the dashboard reads through this DAO.
 *          Validating region here means the controller needs no duplicate guard.
 * ============================================================================
 */
public class CounterpartyDAO {

    private static final Set<String> VALID_REGIONS = Set.of("APAC", "EMEA", "NAMR", "LATAM");

    private static final String SELECT = "SELECT name, lei_code, region FROM counterparties";

    private final DataSource dataSource;

    public CounterpartyDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
    }

    /** Every counterparty, ordered by name. */
    public List<Counterparty> findAll() {
        String sql = SELECT + " ORDER BY name";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Counterparty> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new JdbcException("CounterpartyDAO.findAll failed", e);
        }
    }

    /**
     * Counterparties in the given region, ordered by name.
     *
     * <p>The region is checked against the allowed set BEFORE the query runs —
     * a typo should not cost a database round-trip. An empty list is a valid
     * result (no rows matched); it is never null.
     *
     * @throws IllegalArgumentException if region is null or not one of
     *                                  APAC, EMEA, NAMR, LATAM
     */
    public List<Counterparty> findByRegion(String region) {
        if (region == null || !VALID_REGIONS.contains(region)) {
            throw new IllegalArgumentException(
                    "region must be one of " + VALID_REGIONS + " (was " + region + ")");
        }

        String sql = SELECT + " WHERE region = ? ORDER BY name";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setString(1, region);
            try (ResultSet rs = ps.executeQuery()) {
                List<Counterparty> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new JdbcException("CounterpartyDAO.findByRegion failed: " + region, e);
        }
    }

    private static Counterparty mapRow(ResultSet rs) throws SQLException {
        return Counterparty.builder()
                .name(rs.getString("name"))
                .leiCode(rs.getString("lei_code"))
                .region(rs.getString("region"))
                .build();
    }
}
