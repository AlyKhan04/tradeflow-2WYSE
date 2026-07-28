package com.dbtraining.tradeflow.model;

import java.util.List;

public record Discrepancy(String tradeRef, List<DiscrepancyType> types) {
}
