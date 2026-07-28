package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.exception.JdbcException;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResultDAO — TICKET-I046 (Day 4)
 * ============================================================================
 * WHAT:    JDBC DAO for recon_results.
 * HOW:     PreparedStatement + try-with-resources.
 * WHY:     The matching engine on Day 3 writes results here.
 * ============================================================================
 */
public class ReconResultDAO {

    private static final String SELECT_COLUMNS =
            "SELECT rb.id, rb.trade_id, rb.discrepancy_type, rb.status, " +
            "rb.detected_at, rb.resolved_at, rb.created_at " +
            "FROM recon_breaks rb ";

    private final DataSource dataSource;

    public ReconResultDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public long insert(ReconResult result) {
        String sql = "INSERT INTO recon_breaks (trade_id, discrepancy_type, status, detected_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (result.getTradeId() == null) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, result.getTradeId());
            }
            if (result.getDiscrepancyType() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, result.getDiscrepancyType().name());
            }
            ps.setString(3, result.getStatus());
            ps.setTimestamp(4, result.getDetectedAt() == null ? null : Timestamp.from(result.getDetectedAt()));
            ps.setTimestamp(5, result.getCreatedAt() == null ? null : Timestamp.from(result.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new JdbcException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("ReconResultDAO.insert failed", e);
        }
    }

    public List<ReconResult> findByTradeId(long tradeId) {
        String sql = SELECT_COLUMNS + "WHERE rb.trade_id = ? ORDER BY rb.detected_at DESC";
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
            throw new JdbcException("findByTradeId failed: " + tradeId, e);
        }
    }

    public List<ReconResult> findUnresolved() {
        String sql = SELECT_COLUMNS + "WHERE rb.status = 'OPEN' ORDER BY rb.detected_at DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ReconResult> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new JdbcException("findUnresolved failed", e);
        }
    }

    private static ReconResult mapRow(ResultSet rs) throws SQLException {
        return ReconResult.builder()
                .id(rs.getLong("id"))
                .tradeId(rs.getLong("trade_id"))
                .discrepancyType(rs.getString("discrepancy_type") == null ? null :
                        DiscrepancyType.valueOf(rs.getString("discrepancy_type")))
                .status(rs.getString("status"))
                .detectedAt(rs.getTimestamp("detected_at") != null
                        ? rs.getTimestamp("detected_at").toInstant() : null)
                .resolvedAt(rs.getTimestamp("resolved_at") != null
                        ? rs.getTimestamp("resolved_at").toInstant() : null)
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }
}
