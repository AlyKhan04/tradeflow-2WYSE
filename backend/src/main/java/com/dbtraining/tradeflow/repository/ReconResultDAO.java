package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.exception.JdbcException;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResultDAO — TICKET-I046 (Day 4)
 * ============================================================================
 * WHAT:    JDBC DAO for reconciliation breaks.
 * HOW:     PreparedStatement + try-with-resources.
 * WHY:     The matching engine on Day 3 writes results here.
 *
 * NOTE ON NAMES: the table is `recon_breaks` but the class is `ReconResult`,
 * and the table's timestamp column is `created_at` while the model field is
 * `detectedAt`. Legacy table names drift from current class names in real
 * codebases — mapRow() is where that mismatch gets absorbed.
 * ============================================================================
 */
public class ReconResultDAO {

    private static final String SELECT_COLUMNS = """
            SELECT id, trade_id, status, discrepancy_type, created_at, resolved_at
            FROM recon_breaks
            """;

    private final DataSource dataSource;

    public ReconResultDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
    }

    /** Inserts one break and returns its generated id. */
    public long insert(ReconResult result) {
        String sql = "INSERT INTO recon_breaks (trade_id, discrepancy_type, status) VALUES (?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, result.getTradeId());
            ps.setString(2, result.getDiscrepancyType().name());
            ps.setString(3, result.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new JdbcException("ReconResultDAO.insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("ReconResultDAO.insert failed", e);
        }
    }

    /** All breaks for one trade, newest first. Empty list when none — never null. */
    public List<ReconResult> findByTradeId(long tradeId) {
        String sql = SELECT_COLUMNS + " WHERE trade_id = ? ORDER BY created_at DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setLong(1, tradeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReconResult> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new JdbcException("ReconResultDAO.findByTradeId failed: " + tradeId, e);
        }
    }

    /** Every break still awaiting action, newest first. */
    public List<ReconResult> findUnresolved() {
        String sql = SELECT_COLUMNS + " WHERE status = 'OPEN' ORDER BY created_at DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<ReconResult> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new JdbcException("ReconResultDAO.findUnresolved failed", e);
        }
    }

    private static ReconResult mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");

        return ReconResult.builder()
                .tradeId(rs.getLong("trade_id"))
                .status(rs.getString("status"))
                .discrepancyType(DiscrepancyType.valueOf(rs.getString("discrepancy_type")))
                // DB column is created_at; the model calls the same instant detectedAt.
                .detectedAt(createdAt != null ? createdAt.toInstant() : Instant.now())
                .resolvedAt(resolvedAt != null ? resolvedAt.toInstant() : null)
                .build();
    }
}
