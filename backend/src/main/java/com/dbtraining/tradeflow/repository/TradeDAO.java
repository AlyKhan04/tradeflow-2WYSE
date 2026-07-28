package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.exception.JdbcException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ============================================================================
 * TradeDAO — TICKET-I045 (Day 4 — raw JDBC)
 * ============================================================================
 * WHAT:    Raw JDBC data-access object for trades.
 * HOW:     PreparedStatement everywhere — NEVER string concatenation.
 * WHY:     Day 4 builds JDBC by hand so you understand what JPA hides on Day 5.
 * OBSERVE: Day 5's TradeRepository (Spring Data) replaces this class entirely.
 *          On Day 5 you can either delete this file or keep it for comparison.
 * ============================================================================
 */
public class TradeDAO {

    private static final String SELECT_COLUMNS =
            "id, trade_ref, instrument_id, counterparty_id, quantity, price, " +
            "trade_date, status, created_at ";

    private static final String TABLE = "FROM trades ";

    private final DataSource dataSource;

    public TradeDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public long insert(Trade trade) {
        String sql = "INSERT INTO trades " +
                "(trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trade.getTradeRef());
            ps.setLong(2, trade.getInstrumentId());
            ps.setLong(3, trade.getCounterpartyId());
            ps.setBigDecimal(4, trade.getQuantity());
            ps.setBigDecimal(5, trade.getPrice());
            ps.setDate(6, Date.valueOf(trade.getTradeDate()));
            ps.setString(7, trade.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new JdbcException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("insert failed: " + trade.getTradeRef(), e);
        }
    }

    public Optional<Trade> findByRef(String tradeRef) {
        String sql = "SELECT " + SELECT_COLUMNS + TABLE + "WHERE trade_ref = ? LIMIT 1";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, tradeRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new JdbcException("findByRef failed: " + tradeRef, e);
        }
    }

    public List<Trade> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + TABLE + "ORDER BY trade_date DESC, id DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Trade> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new JdbcException("findAll failed", e);
        }
    }

    public int updateStatus(String tradeRef, TradeStatus newStatus) {
        String sql = "UPDATE trades SET status = ? WHERE trade_ref = ?";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, tradeRef);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException("updateStatus failed: " + tradeRef, e);
        }
    }

    private static Trade mapRow(ResultSet rs) throws SQLException {
        return Trade.builder()
                .id(rs.getLong("id"))
                .tradeRef(rs.getString("trade_ref"))
                .instrumentId(rs.getLong("instrument_id"))
                .counterpartyId(rs.getLong("counterparty_id"))
                .quantity(rs.getBigDecimal("quantity"))
                .price(rs.getBigDecimal("price"))
                .tradeDate(rs.getDate("trade_date").toLocalDate())
                .status(TradeStatus.valueOf(rs.getString("status")))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }
}
