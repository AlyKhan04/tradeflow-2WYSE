package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.exception.JdbcException;
import com.dbtraining.tradeflow.model.Counterparty;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ============================================================================
 * CounterpartyDAO — TICKET-I047 (Day 4)
 * ============================================================================
 */
public class CounterpartyDAO {

    private static final Set<String> VALID_REGIONS = Set.of("APAC", "EMEA", "NAMR", "LATAM");
    private static final String SELECT = "SELECT name, lei_code, region FROM counterparties";

    private final DataSource dataSource;

    public CounterpartyDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

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
            throw new JdbcException("findByRegion failed: " + region, e);
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
