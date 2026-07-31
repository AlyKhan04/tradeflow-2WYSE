package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;

import java.util.ArrayList;
import java.util.List;

public class ReconciliationOrchestrator {

    private final TradeDAO tradeDAO;
    private final ReconResultDAO reconResultDAO;
    private final ReconciliationService reconciliationService;

    public ReconciliationOrchestrator(TradeDAO tradeDAO,
                                     ReconResultDAO reconResultDAO,
                                     ReconciliationService reconciliationService) {
        this.tradeDAO = tradeDAO;
        this.reconResultDAO = reconResultDAO;
        this.reconciliationService = reconciliationService;
    }

    public ReconSummary runForAll() {
        List<Trade> trades = tradeDAO.findAll();
        return runForAll(new ArrayList<>(trades), new ArrayList<>(trades));
    }

    public ReconSummary runForAll(List<Trade> external) {
        List<Trade> trades = tradeDAO.findAll();
        return runForAll(new ArrayList<>(trades), external);
    }

    private ReconSummary runForAll(List<Trade> internal, List<Trade> external) {
        List<BaseTrade> internalBaseTrades = internal.stream().map(this::toBaseTrade).toList();
        List<BaseTrade> externalBaseTrades = external.stream().map(this::toBaseTrade).toList();
        ReconReport report = reconciliationService.matchTrades(internalBaseTrades, externalBaseTrades);
        for (Discrepancy discrepancy : report.discrepancies()) {
            reconResultDAO.insert(buildReconResult(discrepancy));
        }
        return reconciliationService.generateReport(report);
    }

    private ReconResult buildReconResult(Discrepancy discrepancy) {
        Trade trade = Trade.builder().tradeRef("UNKNOWN").build();
        return ReconResult.builder()
                .trade(trade)
                .status(ReconResult.Status.OPEN)
                .discrepancyType(discrepancy.types().get(0))
                .build();
    }

    private BaseTrade toBaseTrade(Trade trade) {
        return new BaseTrade(
                trade.getTradeRef(),
                trade.getInstrumentId(),
                trade.getCounterpartyId(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradeDate(),
                trade.getStatus(),
                trade.getCreatedAt()
        ) {
            @Override
            public String assetClassDescription() {
                return "Trade";
            }
        };
    }
}
