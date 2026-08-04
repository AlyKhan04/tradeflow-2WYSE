package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * AuditLog — TICKET-I059
 * ============================================================================
 * WHAT:    Append-only history of who changed what when.
 * HOW:     JPA entity with JSONB columns for the before/after payload.
 * WHY:     Regulators (and the Ops team) need to be able to reconstruct
 *          every change. Schema agnostic via JSONB.
 * OBSERVE: A trade UPDATE writes one row with old_value+new_value JSON diffs.
 * ============================================================================
 *  TODO(TICKET-I059):
 *    Fields:
 *      Long id
 *      String entity        (e.g. "trade", "recon_result")
 *      Long entityId
 *      String action        (INSERT|UPDATE|DELETE)
 *      String oldValue      (JSON — store as JSONB on Postgres)
 *      String newValue      (JSON — store as JSONB on Postgres)
 *      Instant timestamp
 *      String userName
 *
 *  HINT: For JSONB mapping on Hibernate 6:
 *      @Column(columnDefinition = "jsonb")
 *      @JdbcTypeCode(SqlTypes.JSON)
 *      private String oldValue;
 * ============================================================================
 */
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    public enum Operation { I, U, D }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private Operation operation;

    @Column(name = "row_pk", nullable = false)
    private Long rowPk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;   // nullable — INSERT has no before

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;    // nullable — DELETE has no after

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected AuditLog() {}

    private AuditLog(Builder b) {
        this.tableName  = b.tableName;
        this.operation  = b.operation;
        this.rowPk      = b.rowPk;
        this.beforeData = b.beforeData;
        this.afterData  = b.afterData;
        this.changedBy  = b.changedBy;
        this.changedAt  = b.changedAt != null ? b.changedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tableName;
        private Operation operation;
        private Long rowPk;
        private String beforeData;
        private String afterData;
        private String changedBy;
        private Instant changedAt;

        public Builder tableName(String v)     { this.tableName = v; return this; }
        public Builder operation(Operation v)  { this.operation = v; return this; }
        public Builder rowPk(Long v)           { this.rowPk = v; return this; }
        public Builder beforeData(String v)    { this.beforeData = v; return this; }
        public Builder afterData(String v)     { this.afterData = v; return this; }
        public Builder changedBy(String v)     { this.changedBy = v; return this; }
        public Builder changedAt(Instant v)    { this.changedAt = v; return this; }

        public AuditLog build() { return new AuditLog(this); }
    }

    public Long getId()             { return id; }
    public String getTableName()    { return tableName; }
    public Operation getOperation() { return operation; }
    public Long getRowPk()          { return rowPk; }
    public String getBeforeData()   { return beforeData; }
    public String getAfterData()    { return afterData; }
    public String getChangedBy()    { return changedBy; }
    public Instant getChangedAt()   { return changedAt; }
}
