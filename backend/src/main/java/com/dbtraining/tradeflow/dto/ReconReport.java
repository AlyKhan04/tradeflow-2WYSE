package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.BaseTrade;

import java.util.List;

public record ReconReport(
        int totalInternal,
        int totalExternal,
        List<BaseTrade> matched,
        List<Discrepancy> discrepancies
) {
}
