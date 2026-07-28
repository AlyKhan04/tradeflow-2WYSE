package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final Map<String, BaseTrade> trades = new HashMap<>();

    public Collection<BaseTrade> getAllTrades() {
        return Collections.unmodifiableCollection(trades.values());
    }

    public void addTrade(BaseTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("trade is required");
        }
        if (trade.getTradeRef() == null) {
            throw new IllegalArgumentException("tradeRef is required");
        }
        if (trades.containsKey(trade.getTradeRef())) {
            throw new IllegalStateException("Duplicate tradeRef: " + trade.getTradeRef());
        }
        trades.put(trade.getTradeRef(), trade);
    }

    public Optional<BaseTrade> findByRef(String tradeRef) {
        return Optional.ofNullable(trades.get(tradeRef));
    }

    public Map<Long, BigDecimal> sumByCounterparty() {
        return trades.values().stream()
                .filter(t -> t.getStatus() == TradeStatus.MATCHED)
                .collect(Collectors.groupingBy(
                        BaseTrade::getCounterpartyId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                BaseTrade::getNotional,
                                BigDecimal::add)));
    }

    public List<BaseTrade> topNByValue(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0 (was " + n + ")");
        }
        return trades.values().stream()
                .sorted(Comparator.comparing(BaseTrade::getNotional).reversed()
                        .thenComparing(BaseTrade::getTradeRef))
                .limit(n)
                .toList();
    }

    public TradeDto createTrade(TradeRequest request) {
        throw new UnsupportedOperationException("TICKET-I062");
    }

    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        throw new UnsupportedOperationException("TICKET-I070");
    }

    public void softDelete(Long id) {
        throw new UnsupportedOperationException("TICKET-I071");
    }
}
