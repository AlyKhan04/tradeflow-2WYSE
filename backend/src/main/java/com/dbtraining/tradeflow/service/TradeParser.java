package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.InsufficientDataException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * ============================================================================
 * TradeParser — TICKET-I039
 * ============================================================================
 * WHAT:    Reads CSV (or JSON) and produces a List<BaseTrade>.
 * HOW:     Stream the file line-by-line, parse each row, build a Trade via
 *          its Builder.
 * WHY:     Separation of concerns (SRP): parsing is its own job, distinct
 *          from validation and from reconciliation.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    parseCsv(Path file) -> List<BaseTrade>
 *
 *  HINTS:
 *    - Files.lines(file).skip(1)            // skip header
 *    - .map(line -> line.split(",", -1))    // -1 keeps trailing empties
 *    - Detect asset class from a column to pick subclass.
 *    - Throw InsufficientDataException with line number on a bad row.
 * ============================================================================
 */

@Service
public class TradeParser {

    public List<BaseTrade> parseCsv(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            List<String> rows = lines.toList();
            if (rows.isEmpty()) return List.of();

            List<BaseTrade> out = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {  // skip header
                String line = rows.get(i).trim();
                if (line.isEmpty()) continue;
                try {
                    out.add(parseRow(line));
                } catch (RuntimeException e) {
                    throw new InsufficientDataException(
                            file.getFileName() + ":" + (i + 1) + " — " + e.getMessage(), e);
                }
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BaseTrade parseRow(String line) {
        String[] c = line.split(",", -1);
        if (c.length < 8) {
            throw new InsufficientDataException("expected at least 8 columns, got " + c.length);
        }
        String assetClass     = c[0].trim();
        String tradeRef       = c[1].trim();
        long instrumentId     = parseLong(c[2], "instrument_id");
        long counterpartyId   = parseLong(c[3], "counterparty_id");
        BigDecimal quantity   = parseDecimal(c[4], "quantity");
        BigDecimal price      = parseDecimal(c[5], "price");
        LocalDate tradeDate   = LocalDate.parse(c[6].trim());
        TradeStatus status    = c[7].isBlank() ? TradeStatus.PENDING : TradeStatus.valueOf(c[7].trim());

        return switch (assetClass) {
            case "EQUITY" -> EquityTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .exchange(c.length > 8 ? c[8].trim() : "")
                    .lotSize(c.length > 9 ? (int) parseLong(c[9], "lot_size") : 1)
                    .build();
            case "FX" -> FXTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .baseCurrency(c.length > 10 ? c[10].trim() : "")
                    .quoteCurrency(c.length > 11 ? c[11].trim() : "")
                    .spotRate(c.length > 12 ? parseDecimal(c[12], "spot_rate") : BigDecimal.ZERO)
                    .build();
            case "BOND" -> BondTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .couponRate(c.length > 13 ? parseDecimal(c[13], "coupon_rate") : BigDecimal.ZERO)
                    .maturityDate(c.length > 14 ? LocalDate.parse(c[14].trim()) : tradeDate.plusYears(1))
                    .faceValue(c.length > 15 ? parseDecimal(c[15], "face_value") : BigDecimal.ONE)
                    .build();
            default -> throw new InsufficientDataException("unknown asset_class: " + assetClass);
        };
    }

    private static long parseLong(String raw, String field) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new InsufficientDataException(
                    field + " must be an integer, got '" + raw + "'", e);
        }
    }

    private static BigDecimal parseDecimal(String raw, String field) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new InsufficientDataException(
                    field + " must be a number, got '" + raw + "'", e);
        }
    }
}
