package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ============================================================================
 * EquityTradeValidator — TICKET-I038
 * ============================================================================
 * WHAT:    Validation rules specific to EquityTrade.
 * WHY:     Equity rules differ from FX/Bond (lot size > 0, exchange known).
 * ============================================================================
 *  TODO(TICKET-I038):
 *    - if (!(trade instanceof EquityTrade et)) throw new TradeValidationException(...)
 *    - et.exchange() must be non-blank
 *    - et.lotSize() > 0
 *    - et.quantity() must be a whole multiple of lotSize
 * ============================================================================
 */

@Component
public class EquityTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof EquityTrade et)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "EquityTradeValidator only accepts EquityTrade");
        }
        if (et.getExchange() == null || et.getExchange().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "exchange is required");
        }
        if (et.getLotSize() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "lotSize must be > 0");
        }
        BigDecimal lotSize = BigDecimal.valueOf(et.getLotSize());
        if (et.getQuantity().remainder(lotSize).signum() != 0) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "quantity must be a whole multiple of lotSize=" + et.getLotSize());
        }
    }
}
