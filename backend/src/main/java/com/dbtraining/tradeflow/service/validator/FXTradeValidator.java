package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import org.springframework.stereotype.Component;

@Component
public class FXTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof FXTrade fx)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "FXTradeValidator only accepts FXTrade");
        }
        if (fx.getBaseCurrency() == null || fx.getBaseCurrency().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "baseCurrency is required");
        }
        if (fx.getQuoteCurrency() == null || fx.getQuoteCurrency().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "quoteCurrency is required");
        }
        if (fx.getSpotRate() == null || fx.getSpotRate().signum() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "spotRate must be > 0");
        }
    }
}
