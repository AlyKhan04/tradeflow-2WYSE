package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.service.TradeProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

@SpringBootApplication
public class TradeflowApplication {

    public static void main(String[] args) {
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
}