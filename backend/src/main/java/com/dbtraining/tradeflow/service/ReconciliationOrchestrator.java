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

    public ReconSummary runForAll(List<BaseTrade> external) {
        List<Trade> trades = tradeDAO.findAll();
        return runForAll(new ArrayList<>(trades), external);
    }

    private ReconSummary runForAll(List<BaseTrade> internal, List<BaseTrade> external) {
        ReconReport report = reconciliationService.matchTrades(internal, external);
        for (Discrepancy discrepancy : report.discrepancies()) {
            reconResultDAO.insert(buildReconResult(discrepancy));
        }
        return reconciliationService.generateReport(report);
    }

    private ReconResult buildReconResult(Discrepancy discrepancy) {
        return new ReconResult.Builder()
                .tradeId(1L)
                .status("OPEN")
                .discrepancyType(discrepancy.types().get(0))
                .build();
    }
}
