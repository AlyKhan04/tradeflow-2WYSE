package com.dbtraining.tradeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * TradeflowApplication — Spring Boot entry point
 * ============================================================================
 * WHAT:    The single annotated main() that bootstraps the whole service.
 * HOW:     `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration`
 *          + `@ComponentScan` — scans this package and below for beans.
 * WHY:     One starting point, predictable lifecycle, easy to launch from
 *          IDE or `./mvnw spring-boot:run`.
 * OBSERVE: Boot log includes "Started TradeflowApplication in X seconds".
 * ============================================================================
 *  Tickets that touch this file:
 *   - TICKET-I016 — package structure + boot main
 *   - TICKET-I026 — print formatted trade list (Day 2, BEFORE Spring boot wiring)
 *   - TICKET-I040 — wire up the full recon pipeline run in main (Day 3 sprint)
 *
 *  Note: I026 runs BEFORE we have Spring Boot — for Day 2 you'll use a plain
 *  `public static void main` without `@SpringBootApplication`. From Day 5
 *  onward, this becomes the Spring Boot entry-point as below.
 * ============================================================================
 */
@SpringBootApplication
public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        // Day 2: run a plain Java console demo of the domain model instead of starting Spring
        printDay2Demo();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ████████ ██████   █████  ██████  ███████ ███████ ██       ██████  ██     ██");
        System.out.println("     ██    ██   ██ ██   ██ ██   ██ ██      ██      ██      ██    ██ ██     ██");
        System.out.println("     ██    ██████  ███████ ██   ██ █████   █████   ██      ██    ██ ██  █  ██");
        System.out.println("     ██    ██   ██ ██   ██ ██   ██ ██      ██      ██      ██    ██ ██ ███ ██");
        System.out.println("     ██    ██   ██ ██   ██ ██████  ███████ ██      ███████  ██████   ███ ███");
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }

    /** TICKET-I026 — Console demo of the domain model independent of the DB. */
    private static void printDay2Demo() {
        System.out.println("== Day-2 domain-model demo (TICKET-I026) ===========================================");
        System.out.println();

        var trades = java.util.List.of(
            com.dbtraining.tradeflow.model.Trade.builder()
                .tradeRef("TRD-1001")
                .instrumentId(101L)
                .counterpartyId(201L)
                .quantity(new java.math.BigDecimal("1000"))
                .price(new java.math.BigDecimal("152.40"))
                .tradeDate(java.time.LocalDate.of(2026,3,12))
                .status(com.dbtraining.tradeflow.model.TradeStatus.MATCHED)
                .build(),
            com.dbtraining.tradeflow.model.Trade.builder()
                .tradeRef("TRD-1002")
                .instrumentId(102L)
                .counterpartyId(202L)
                .quantity(new java.math.BigDecimal("250"))
                .price(new java.math.BigDecimal("99.99"))
                .tradeDate(java.time.LocalDate.of(2026,3,13))
                .status(com.dbtraining.tradeflow.model.TradeStatus.PENDING)
                .build(),
            com.dbtraining.tradeflow.model.Trade.builder()
                .tradeRef("TRD-1003")
                .instrumentId(103L)
                .counterpartyId(203L)
                .quantity(new java.math.BigDecimal("500"))
                .price(new java.math.BigDecimal("10.00"))
                .tradeDate(java.time.LocalDate.of(2026,3,14))
                .status(com.dbtraining.tradeflow.model.TradeStatus.UNMATCHED)
                .build(),
            com.dbtraining.tradeflow.model.Trade.builder()
                .tradeRef("TRD-1004")
                .instrumentId(104L)
                .counterpartyId(204L)
                .quantity(new java.math.BigDecimal("1"))
                .price(new java.math.BigDecimal("1000000.00"))
                .tradeDate(java.time.LocalDate.of(2026,3,15))
                .status(com.dbtraining.tradeflow.model.TradeStatus.DISPUTED)
                .build(),
            com.dbtraining.tradeflow.model.Trade.builder()
                .tradeRef("TRD-1005")
                .instrumentId(105L)
                .counterpartyId(205L)
                .quantity(new java.math.BigDecimal("10"))
                .price(new java.math.BigDecimal("1.23"))
                .tradeDate(java.time.LocalDate.of(2026,3,16))
                .status(com.dbtraining.tradeflow.model.TradeStatus.CANCELLED)
                .build()
        );

        // Print header
        String fmt = "%-12s | %-12s | %-6s | %12s | %12s | %-10s | %-9s";
        System.out.println(String.format(fmt, "TRADE_REF", "INSTRUMENT_ID", "CP_ID", "QTY", "PRICE", "DATE", "STATUS"));
        System.out.println("-------------------------------------------------------------------------------");
        for (var t : trades) {
            System.out.println(String.format(fmt,
                    t.getTradeRef(),
                    t.getInstrumentId() != null ? t.getInstrumentId().toString() : "-",
                    t.getCounterpartyId() != null ? t.getCounterpartyId().toString() : "-",
                    t.getQuantity() != null ? t.getQuantity().toPlainString() : "-",
                    t.getPrice() != null ? t.getPrice().toPlainString() : "-",
                    t.getTradeDate() != null ? t.getTradeDate().toString() : "-",
                    t.getStatus() != null ? t.getStatus().name() : "-"
            ));
        }
        System.out.println();
    }
}
