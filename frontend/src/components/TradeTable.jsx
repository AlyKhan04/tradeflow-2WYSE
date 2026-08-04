/**
 * ============================================================================
 * TradeTable.jsx — TICKET-I104
 * ============================================================================
 * WHAT:    Renders a list of trades as a sortable, filterable table.
 * HOW:     Pure presentational — receives data + handlers via props.
 * WHY:     Keep data fetching OUT of components when possible — easier to
 *          test, reuse, and reason about.
 * ============================================================================
 *
 *  Props:
 *    trades       : Trade[]
 *    sortField    : string
 *    sortDir      : 'asc' | 'desc'
 *    onSortChange : (field) => void
 *    loading      : boolean
 *
 *  TODO(TICKET-I104):
 *    - render rows from `trades`
 *    - clickable <th> calls onSortChange(field)
 *    - show ▲/▼ next to the active sort field
 *    - on empty list, show a "no trades match" placeholder
 *
 *  HINT: key={t.id} on every row. Missing keys cause subtle re-render bugs.
 * ============================================================================
 */
import StatusBadge from './StatusBadge.jsx';

const PAGE_SIZE = 20;

const COLUMNS = [
    { key: 'tradeRef',       label: 'Trade Ref' },
    { key: 'instrumentId',   label: 'Instrument' },
    { key: 'counterpartyId', label: 'Counterparty' },
    { key: 'quantity',       label: 'Qty' },
    { key: 'price',          label: 'Price' },
    { key: 'tradeDate',      label: 'Date' },
    { key: 'status',         label: 'Status' },
];

export default function TradeTable({
    trades = [],
    sortField,
    sortDir,
    onSortChange,
    loading,
    page = 0,
    pageSize = PAGE_SIZE,
    onPageChange
}) {
    if (loading) return <div className="loading">Loading trades…</div>;
    if (!trades.length) return <div className="empty">No trades match your filters.</div>;

    const start = page * pageSize;
    const pageRows = trades.slice(start, start + pageSize);
    const totalPages = Math.max(1, Math.ceil(trades.length / pageSize));

    return (
        <>
            <table className="data-table">
                <thead>
                    <tr>
                        {COLUMNS.map(col => (
                            <th key={col.key}
                                onClick={() => onSortChange?.(col.key)}
                                style={{ cursor: onSortChange ? 'pointer' : 'default' }}>
                                {col.label}
                                {sortField === col.key && (sortDir === 'asc' ? ' ▲' : ' ▼')}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {pageRows.map(t => (
                        <tr key={t.id || t.tradeRef}>
                            <td>{t.tradeRef}</td>
                            <td>{t.instrumentId}</td>
                            <td>{t.counterpartyId}</td>
                            <td>{t.quantity}</td>
                            <td>{t.price}</td>
                            <td>{t.tradeDate}</td>
                            <td><StatusBadge status={t.status} /></td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {onPageChange && totalPages > 1 && (
                <div className="pagination">
                    <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>Prev</button>
                    <span>Page {page + 1} of {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Next</button>
                </div>
            )}
        </>
    );
}