package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import com.dbtraining.tradeflow.model.Counterparty;
import com.dbtraining.tradeflow.model.Instrument;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.CounterpartyRepository;
import com.dbtraining.tradeflow.repository.InstrumentRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Page<TradeDto> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::from);
    }

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

    @Transactional
    public TradeDto createTrade(TradeRequest request) {
        if (tradeRepository.existsByTradeRef(request.tradeRef())) {
            throw new IllegalStateException("Trade with tradeRef '" + request.tradeRef() + "' already exists");
        }
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "instrumentId " + request.instrumentId() + " not found"));
        Counterparty counterparty = counterpartyRepository.findById(request.counterpartyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "counterpartyId " + request.counterpartyId() + " not found"));

        Trade trade = Trade.builder()
                .tradeRef(request.tradeRef())
                .instrument(instrument)
                .counterparty(counterparty)
                .quantity(request.quantity())
                .price(request.price())
                .tradeDate(request.tradeDate())
                .status(TradeStatus.PENDING)
                .build();

        Trade saved = tradeRepository.save(trade);
        return TradeDto.from(saved);
    }

    @Transactional
    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        if (trade.getStatus() != null && trade.getStatus().isTerminal()) {
            throw new IllegalStateException(
                    "Trade " + id + " is in terminal status " + trade.getStatus() + " and cannot transition");
        }
        trade.setStatus(newStatus);
        return TradeDto.from(trade);
    }

    @Transactional
    public void softDelete(Long id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        trade.setStatus(TradeStatus.CANCELLED);
        // JPA dirty-checking flushes the UPDATE at commit — no explicit save() needed.
    }
}