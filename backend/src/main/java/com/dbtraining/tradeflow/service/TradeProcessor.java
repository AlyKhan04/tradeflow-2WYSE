package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * ============================================================================
 * TradeProcessor — TICKET-I039
 * ============================================================================
 * WHAT:    Top-level orchestrator: parse → validate → reconcile → export.
 * WHY:     SRP — this class composes the others; it doesn't parse, validate,
 *          or reconcile itself.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    Constructor takes TradeParser, TradeValidator, ReconciliationService,
 *    ReconReportExporter.
 *
 *    Method: void process(Path internal, Path external, Path outputCsv)
 *      - parse both files
 *      - validate both lists
 *      - matchTrades + generateReport
 *      - export discrepancies to CSV
 *      - log the ReconSummary to stdout
 * ============================================================================
 */
@Service
public class TradeProcessor {

    private static final Logger log = LoggerFactory.getLogger(TradeProcessor.class);

    private final TradeParser parser;
    private final TradeValidator validator;
    private final ReconciliationService recon;
    private final ReconReportExporter exporter;

    public TradeProcessor(TradeParser parser, TradeValidator validator,
                          ReconciliationService recon, ReconReportExporter exporter) {
        this.parser = parser; this.validator = validator;
        this.recon  = recon;  this.exporter  = exporter;
    }

    public ReconSummary process(Path internalCsv, Path externalCsv) {
        log.info("Parsing internal feed: {}", internalCsv);
        List<BaseTrade> internal = parser.parseCsv(internalCsv);
        log.info("Parsing external feed: {}", externalCsv);
        List<BaseTrade> external = parser.parseCsv(externalCsv);

        for (TradeValidationException finding : validator.validateAll(internal)) {
            log.warn("internal validation finding: [{}] {}", finding.getCode(), finding.getMessage());
        }
        for (TradeValidationException finding : validator.validateAll(external)) {
            log.warn("external validation finding: [{}] {}", finding.getCode(), finding.getMessage());
        }

        ReconReport report = recon.matchTrades(internal, external);
        ReconSummary summary = recon.generateReport(report);
        log.info("\n{}", recon.render(summary));
        return summary;
    }
}
