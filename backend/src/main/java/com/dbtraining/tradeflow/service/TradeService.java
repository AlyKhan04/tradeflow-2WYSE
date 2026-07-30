package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================================================
 * TradeService — TICKET-I041..I043 + TICKET-I062
 * ============================================================================
 * WHAT:    Business-logic facade for Trade operations.
 *          Day 4: HashMap-backed + Streams pipelines.
 *          Day 5: rewritten to use TradeRepository (Spring Data JPA).
 *          Day 6: also publishes TradeEvent to Kafka (TICKET-I115).
 * HOW:     @Service from Day 1 (so Spring can wire it into controllers).
 *          Day-1 default is a no-op stub — controllers bypass it via
 *          JdbcTemplate. Day-4 onward, students replace the stubs.
 * WHY:     Controllers stay thin — all rules and persistence live here.
 * OBSERVE: Switching from HashMap to JPA on Day 5 should NOT require changing
 *          callers (controller code stays the same).
 * ============================================================================
 *
 *  TICKET-I041: refactor in-memory store to Map<String, BaseTrade>.
 *  TICKET-I042: Streams pipeline — sumByCounterparty.
 *  TICKET-I043: Streams pipeline — topNByValue.
 *  TICKET-I062: rewrite using JPA repositories + DTOs (Day 5).
 * ============================================================================
 */
@Service
public class TradeService {

    private final TradeDAO tradeDAO;

    public TradeService(TradeDAO tradeDAO) {
        this.tradeDAO = tradeDAO;
    }

    public List<Trade> getAllTrades() {
        return tradeDAO.findAll();
    }

    public TradeDto createTrade(TradeRequest request) {
        Trade trade = Trade.builder()
                .tradeRef(request.tradeRef())
                .instrumentId(request.instrumentId())
                .counterpartyId(request.counterpartyId())
                .quantity(request.quantity())
                .price(request.price())
                .tradeDate(request.tradeDate())
                .status(TradeStatus.PENDING)
                .build();

        long id = tradeDAO.insert(trade);
        Trade saved = tradeDAO.findById(id)
                .orElseThrow(() -> new IllegalStateException("Created trade not found: " + id));
        return TradeDto.from(saved);
    }

    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        int updated = tradeDAO.updateStatusById(id, newStatus);
        if (updated == 0) {
            throw new IllegalArgumentException("Trade not found: " + id);
        }
        return TradeDto.from(tradeDAO.findById(id)
                .orElseThrow(() -> new IllegalStateException("Updated trade not found: " + id)));
    }

    public void softDelete(Long id) {
        int updated = tradeDAO.softDeleteById(id);
        if (updated == 0) {
            throw new IllegalArgumentException("Trade not found: " + id);
        }
    }
}
