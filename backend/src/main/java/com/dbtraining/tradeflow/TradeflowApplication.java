package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================================
 * TradeflowApplication — Spring Boot entry point
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

    @Bean
    CommandLineRunner reconDemoRunner(TradeProcessor processor) {
        return args -> {
            Path internal = Path.of("src/test/resources/internal-trades.csv");
            Path external = Path.of("src/test/resources/external-trades.csv");
            ReconSummary summary = processor.process(internal, external);
            System.out.println();
            System.out.println("== Day-3 recon demo (TICKET-I040) ==================================================");
            System.out.println(summary);
            System.out.println("====================================================================================");
        };
    }

    private static void verifyTradeEquality() {
        Trade a = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("50.00"))
                .tradeDate(LocalDate.now()).status(TradeStatus.PENDING)
                .build();

        Trade b = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(2L).counterpartyId(9L)
                .quantity(new BigDecimal("200")).price(new BigDecimal("99.99"))
                .tradeDate(LocalDate.now()).status(TradeStatus.MATCHED)
                .build();

        Trade c = Trade.builder()
                .tradeRef("TRD-2")
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
