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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final Map<String, Trade> tradesByRef = new HashMap<>();

    public TradeService(TradeRepository tradeRepository,
                        InstrumentRepository instrumentRepository,
                        CounterpartyRepository counterpartyRepository) {
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::from);
    }

    public Collection<Trade> getAllTrades() {
        return Collections.unmodifiableCollection(tradesByRef.values());
    }

    public void addTrade(Trade trade) {
        if (tradesByRef.containsKey(trade.getTradeRef())) {
            throw new IllegalStateException(
                    "Duplicate tradeRef: " + trade.getTradeRef());
        }
        tradesByRef.put(trade.getTradeRef(), trade);
    }

    public Optional<Trade> findByRef(String tradeRef) {
        return Optional.ofNullable(tradesByRef.get(tradeRef));
    }

    public Map<Long, BigDecimal> sumByCounterparty() {
        return tradesByRef.values().stream()
                .filter(t -> t.getStatus() == TradeStatus.MATCHED)
                .map(t -> Map.entry(t.getCounterpartyId(), t.getNotional()))
                .reduce(new HashMap<Long, BigDecimal>(), (acc, entry) -> {
                    acc.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                    return acc;
                }, (left, right) -> {
                    right.forEach((key, value) -> left.merge(key, value, BigDecimal::add));
                    return left;
                });
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