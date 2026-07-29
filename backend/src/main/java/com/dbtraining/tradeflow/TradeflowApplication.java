package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    /** Shared by the header and every data row, so columns align by construction. */
    private static final String ROW_FORMAT =
            "%-15s | %-13s | %-5s | %10s | %10s | %-12s | %-10s%n";

    public static void main(String[] args) {
        printBanner();
        verifyTradeEquality();
        printDay2Demo();
        SpringApplication.run(TradeflowApplication.class, args);
    }

    /**
     * TICKET-I025 — the AC's "manual assertion in main".
     *
     * Builds two Trades that share a tradeRef but differ in every other
     * field, and proves they are equal with matching hash codes. Fails loudly
     * at startup rather than silently corrupting Day 3's HashMap dedup.
     */
    private static void verifyTradeEquality() {
        Trade a = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("50.00"))
                .tradeDate(LocalDate.now()).status(TradeStatus.PENDING)
                .build();

        Trade b = Trade.builder()
                .tradeRef("TRD-1")                    // same business key ...
                .instrumentId(2L).counterpartyId(9L)  // ... everything else differs
                .quantity(new BigDecimal("200")).price(new BigDecimal("99.99"))
                .tradeDate(LocalDate.now()).status(TradeStatus.MATCHED)
                .build();

        Trade c = Trade.builder()
                .tradeRef("TRD-2")                    // different business key
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("50.00"))
                .tradeDate(LocalDate.now()).status(TradeStatus.PENDING)
                .build();

        if (!a.equals(b))                 throw new AssertionError("equals broken: same tradeRef must be equal");
        if (a.hashCode() != b.hashCode()) throw new AssertionError("hashCode broken: equal objects must share a hash");
        if (a.equals(c))                  throw new AssertionError("equals broken: different tradeRef must not be equal");

        System.out.println("  [I025] equals/hashCode contract verified on tradeRef.");
        System.out.println();
    }

    /**
     * TICKET-I026 — console demo of the domain model, independent of the DB.
     *
     * Proves the model compiles and constructs end-to-end before Day 4 wires
     * JDBC. Trades are hardcoded on purpose: there is no persistence yet.
     *
     * The header and the data rows share ROW_FORMAT, so the columns line up
     * by construction rather than by counting spaces. In printf, "%-15s" is a
     * string left-aligned in a minimum width of 15; dropping the "-" right-
     * aligns it, which is why the numeric columns read right and the labels
     * read left. "%n" emits the platform-correct newline (prefer it to "\n").
     */
    private static void printDay2Demo() {
        List<Trade> trades = List.of(
                Trade.builder().tradeRef("TRD-2026-0001")
                        .instrumentId(1L).counterpartyId(1L)
                        .quantity(new BigDecimal("1000.00")).price(new BigDecimal("245.50"))
                        .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.MATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0002")
                        .instrumentId(1L).counterpartyId(2L)
                        .quantity(new BigDecimal("500.00")).price(new BigDecimal("246.00"))
                        .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.UNMATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0003")
                        .instrumentId(2L).counterpartyId(1L)
                        .quantity(new BigDecimal("100000.00")).price(new BigDecimal("99.50"))
                        .tradeDate(LocalDate.of(2026, 3, 2)).status(TradeStatus.MATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0004")
                        .instrumentId(3L).counterpartyId(2L)
                        .quantity(new BigDecimal("10.00")).price(new BigDecimal("2125.75"))
                        .tradeDate(LocalDate.of(2026, 3, 3)).status(TradeStatus.DISPUTED).build(),
                Trade.builder().tradeRef("TRD-2026-0005")
                        .instrumentId(2L).counterpartyId(3L)
                        .quantity(new BigDecimal("750.00")).price(new BigDecimal("100.25"))
                        .tradeDate(LocalDate.of(2026, 3, 4)).status(TradeStatus.PENDING).build()
        );

        System.out.println("== Day-2 domain-model demo (TICKET-I026) ==========================================");
        System.out.printf(ROW_FORMAT,
                "TRADE_REF", "INSTRUMENT_ID", "CP_ID", "QTY", "PRICE", "DATE", "STATUS");
        System.out.println("-".repeat(95));

        trades.forEach(t -> System.out.printf(ROW_FORMAT,
                t.getTradeRef(),
                t.getInstrumentId(),
                t.getCounterpartyId(),
                t.getQuantity(),
                t.getPrice(),
                t.getTradeDate(),
                t.getStatus()));

        System.out.println("=".repeat(95));
        System.out.println();
    }

    private static void printBanner() {
        // TICKET-I026 note: the guide suggests stripping @SpringBootApplication
        // to make main() plain Java for Day 2. We deliberately did not -- Day 2's
        // Sprint 3 (Liquibase, I009/I010) needs the app to boot to run the
        // migrations. Printing before SpringApplication.run satisfies I026's AC
        // without breaking that.
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
}
