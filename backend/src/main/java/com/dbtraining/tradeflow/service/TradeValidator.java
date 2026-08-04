package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.service.validator.BondTradeValidator;
import com.dbtraining.tradeflow.service.validator.EquityTradeValidator;
import com.dbtraining.tradeflow.service.validator.FXTradeValidator;
import com.dbtraining.tradeflow.service.validator.ITradeValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * TradeValidator — TICKET-I039
 * ============================================================================
 * WHAT:    Orchestrator that picks the right ITradeValidator per asset class
 *          and validates a batch.
 * WHY:     Open/Closed Principle — adding a new asset class means a new
 *          validator, NOT modifying this class.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    - Inject Map<AssetClass, ITradeValidator> validators
 *    - validateAll(List<BaseTrade>) returns List<TradeValidationException> (or
 *      throws if you prefer fail-fast — discuss with team).
 * ============================================================================
 */
@Service
public class TradeValidator {

    private final Map<Class<? extends BaseTrade>, ITradeValidator> strategies;

    public TradeValidator(EquityTradeValidator equity,
                          FXTradeValidator fx,
                          BondTradeValidator bond) {
        this.strategies = Map.of(
                EquityTrade.class, equity,
                FXTrade.class,     fx,
                BondTrade.class,   bond
        );
    }

    public List<TradeValidationException> validateAll(List<BaseTrade> trades) {
        List<TradeValidationException> findings = new ArrayList<>();
        for (BaseTrade t : trades) {
            ITradeValidator v = strategies.get(t.getClass());
            if (v == null) {
                findings.add(new TradeValidationException(
                        TradeValidationException.Code.INVALID_VALUE,
                        "no validator registered for " + t.getClass().getSimpleName()
                                + " (tradeRef=" + t.getTradeRef() + ")"));
                continue;
            }
            try {
                v.validate(t);
            } catch (TradeValidationException ex) {
                findings.add(ex);
            }
        }
        return findings;
    }
}