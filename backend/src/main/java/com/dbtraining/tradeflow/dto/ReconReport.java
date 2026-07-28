package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.Discrepancy;

import java.util.List;

public record ReconReport(
        List<BaseTrade> matched,
        List<Discrepancy> discrepancies,
        int totalInternal,
        int totalExternal
) {
}
