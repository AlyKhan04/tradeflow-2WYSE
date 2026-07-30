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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.LocalDate;
import java.util.List;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final Counter tradesCreatedCounter;

    public TradeService(TradeRepository tradeRepository,
                        InstrumentRepository instrumentRepository,
                        CounterpartyRepository counterpartyRepository,
                        MeterRegistry meterRegistry) {
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;

        this.tradesCreatedCounter = Counter.builder("tradeflow_trades_created_total")
                .description("Total trades successfully created via POST /api/v1/trades")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findAll() {
        return tradeRepository.findAll().stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public TradeDto findById(Long id) {
        return tradeRepository.findById(id).map(TradeDto::from)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status).stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findPageByStatus(TradeStatus status, Pageable pageable) {
        return tradeRepository.findByStatus(status, pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByDateRange(LocalDate from, LocalDate to) {
        return tradeRepository.findByTradeDateBetween(from, to).stream().map(TradeDto::from).toList();
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
        tradesCreatedCounter.increment();
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