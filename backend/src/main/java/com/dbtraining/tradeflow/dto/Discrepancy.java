package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;

import java.util.List;

public record Discrepancy(String tradeRef, List<DiscrepancyType> types) {
}
