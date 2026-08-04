package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import org.springframework.stereotype.Component;

@Component
public class BondTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof BondTrade bond)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "BondTradeValidator only accepts BondTrade");
        }
        if (bond.getCouponRate() == null) {
            throw new TradeValidationException(Code.MISSING_FIELD, "couponRate is required");
        }
        if (bond.getCouponRate().signum() < 0 || bond.getCouponRate().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "couponRate must be between 0 and 100");
        }
        if (bond.getMaturityDate() == null || !bond.getMaturityDate().isAfter(bond.getTradeDate())) {
            throw new TradeValidationException(Code.INVALID_VALUE, "maturityDate must be after tradeDate");
        }
        if (bond.getFaceValue() == null || bond.getFaceValue().signum() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "faceValue must be > 0");
        }
    }
}
