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

    private final Map<String, BaseTrade> tradesByRef = new HashMap<>();

    // TODO(TICKET-I062) [Day 5]: replace the Map with TradeRepository injection:
    //   private final TradeRepository tradeRepository;
    //   public TradeService(TradeRepository tradeRepository) { ... }

    public Collection<BaseTrade> getAllTrades() {
        return Collections.unmodifiableCollection(tradesByRef.values());
    }

    public void addTrade(BaseTrade trade) {
        if (tradesByRef.containsKey(trade.getTradeRef())) {
            throw new IllegalStateException(
                    "Duplicate tradeRef: " + trade.getTradeRef());
        }
        tradesByRef.put(trade.getTradeRef(), trade);
    }

    public Optional<BaseTrade> findByRef(String tradeRef) {
        return Optional.ofNullable(tradesByRef.get(tradeRef));
    }

    public Map<Long, BigDecimal> sumByCounterparty() {
        return sumByCounterparty(tradesByRef.values().stream().toList());
    }

    public Map<Long, BigDecimal> sumByCounterparty(List<BaseTrade> input) {
        return input.stream()
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
        return tradesByRef.values().stream()
                .sorted(Comparator.comparing(BaseTrade::getNotional).reversed()
                        .thenComparing(BaseTrade::getTradeRef))
                .limit(n)
                .toList();
    }

    /**
     * TODO(TICKET-I062) [Day 5]:
     *   Convert TradeRequest -> Trade entity, save via TradeRepository,
     *   publish TradeEvent on success (TICKET-I115), return TradeDto.
     */
    public TradeDto createTrade(TradeRequest request) {
        throw new UnsupportedOperationException("TICKET-I062");
    }

    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        // TODO(TICKET-I070): implement on Day 6.
        throw new UnsupportedOperationException("TICKET-I070");
    }

    public void softDelete(Long id) {
        // TODO(TICKET-I071): implement soft delete + audit log on Day 6.
        throw new UnsupportedOperationException("TICKET-I071");
    }
}
