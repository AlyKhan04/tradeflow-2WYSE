package com.dbtraining.tradeflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * ============================================================================
 * DatabaseConfig — TICKET-I044 (Day 4 — pre-Spring Boot)
 * ============================================================================
 * WHAT:    Standalone connection pool config for Day-4 JDBC work.
 * HOW:     Build a HikariDataSource from environment / properties.
 * WHY:     Day 4 uses raw JDBC — without a pool you'd open + close a
 *          connection per call, which is brutal under any load.
 * ============================================================================
 */
public final class DatabaseConfig {

    private static volatile DataSource instance;

    private DatabaseConfig() {}

    /**
     * TICKET-I044: Lazily-built singleton DataSource backed by HikariCP.
     */
    public static DataSource dataSource() {
        DataSource local = instance;
        if (local == null) {
            synchronized (DatabaseConfig.class) {
                local = instance;
                if (local == null) {
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl(System.getenv().getOrDefault(
                            "JDBC_URL", "jdbc:h2:mem:tradeflow;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"));
                    cfg.setUsername(System.getenv().getOrDefault("POSTGRES_USER", "sa"));
                    cfg.setPassword(System.getenv().getOrDefault("POSTGRES_PASSWORD", ""));
                    cfg.setMaximumPoolSize(10);
                    cfg.setConnectionTimeout(5_000);
                    cfg.setPoolName("tradeflow-jdbc");
                    instance = local = new HikariDataSource(cfg);
                }
            }
        }
        return local;
    }
}
