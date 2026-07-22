# TradeFlow — Entity Relationship Diagram

```mermaid
erDiagram
    COUNTERPARTIES ||--o{ TRADES        : "originates"
    INSTRUMENTS    ||--o{ TRADES        : "references"
    TRADES         ||--o{ SETTLEMENTS   : "settles"
    TRADES         ||--o{ RECON_BREAKS  : "may produce"

    COUNTERPARTIES {
        BIGINT id PK
        VARCHAR name
        CHAR(20) lei_code UK
        VARCHAR region "APAC|EMEA|NAMR|LATAM"
    }
    INSTRUMENTS {
        BIGINT id PK
        VARCHAR symbol UK
        VARCHAR name
        VARCHAR asset_class "EQUITY|FIXED_INCOME|FX|COMMODITY|DERIVATIVE"
        CHAR(3) currency
        CHAR(12) isin UK
    }
    TRADES {
        BIGINT id PK
        VARCHAR trade_ref UK
        BIGINT instrument_id FK
        BIGINT counterparty_id FK
        NUMERIC quantity "(18,4) > 0"
        NUMERIC price "(18,4) >= 0"
        DATE trade_date
        VARCHAR status "PENDING|MATCHED|UNMATCHED|DISPUTED|CANCELLED"
        TIMESTAMPTZ created_at
    }
    SETTLEMENTS {
        BIGINT id PK
        BIGINT trade_id FK
        DATE settlement_date
        NUMERIC amount "(18,4) >= 0"
        VARCHAR status "PENDING|SETTLED|FAILED|CANCELLED"
    }
    RECON_BREAKS {
        BIGINT id PK
        BIGINT trade_id FK
        VARCHAR discrepancy_type "PRICE|QUANTITY|MISSING|DUPLICATE|STATUS"
        VARCHAR status "OPEN|INVESTIGATING|RESOLVED|IGNORED"
        TIMESTAMPTZ resolved_at
    }
```
