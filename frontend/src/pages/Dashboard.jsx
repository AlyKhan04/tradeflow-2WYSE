/**
 * ============================================================================
 * Dashboard.jsx — TICKET-I103
 * ============================================================================
 * WHAT:    Landing page with 4 summary cards.
 * WHY:     The number Ops users look at first thing in the morning.
 * ============================================================================
 *
 *  DONE(TICKET-I103):
 *    - useTradeData + useReconResults
 *    - derive: total trades, matched %, unmatched count, avg processing time
 *    - refresh every 30s (useEffect + setInterval, cleared on unmount)
 * ============================================================================
 */

import { useEffect, useMemo } from 'react';
import StatCard from '../components/StatCard.jsx';
import { useTradeData } from '../hooks/useTradeData.js';
import { useReconResults } from '../hooks/useReconResults.js';

export default function Dashboard() {
    const filters = useMemo(() => ({ size: 500 }), []);
    const { trades, loading, refetch: refetchTrades } = useTradeData(filters);
    const { results: openBreaks, refetch: refetchBreaks } = useReconResults('OPEN');
    const { results: resolvedBreaks } = useReconResults('RESOLVED');


    useEffect(() => {
        const id = setInterval(() => {
            refetchTrades();
            refetchBreaks();
        }, 30_000);

        return () => clearInterval(id);
    }, [refetchTrades, refetchBreaks]);

    const total = trades.length;
    const matched = trades.filter(t => t.status === 'MATCHED').length;
    const matchedPct = total ? Math.round((matched / total) * 100) + '%' : '—';
    const avgHours = computeAvgResolutionHours(resolvedBreaks);

    return (
        <>
            <h1>Operations Dashboard</h1>

            <section className="cards">
                <StatCard caption="Total Trades"        value={loading ? '…' : total} />
                <StatCard caption="Matched %"           value={loading ? '…' : matchedPct} />
                <StatCard caption="Unmatched Count"     value={openBreaks.length} />
                <StatCard caption="Avg Resolution Hrs" value={avgHours} />
            </section>
        </>
    );
}

function computeAvgResolutionHours(resolved) {
    if (!resolved || resolved.length === 0) return '—';

    const valid = resolved.filter(r => r.detectedAt && r.resolvedAt);

    if (valid.length === 0) return '—';

    const totalHrs = valid.reduce((acc, r) => {
        const ms = new Date(r.resolvedAt) - new Date(r.detectedAt);
        return acc + ms / 3_600_000;
    }, 0);

    return (totalHrs / valid.length).toFixed(1) + 'h';
}