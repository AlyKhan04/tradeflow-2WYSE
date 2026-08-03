import { useState } from 'react';
import StatusBadge from '../components/StatusBadge.jsx';
import ResolveBreakModal from '../components/ResolveBreakModal.jsx';
import { useReconResults } from '../hooks/useReconResults.js';
import { resolveBreak } from '../services/apiService.js';

export default function Recon() {
    const [filter, setFilter] = useState('OPEN');
    const { results, loading, error, refetch } = useReconResults(filter);
    const [optimistic, setOptimistic] = useState({});
    const [selectedBreak, setSelectedBreak] = useState(null);
    const [resolutionNote, setResolutionNote] = useState('');
    const [modalError, setModalError] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    const openModal = (breakItem) => {
        setSelectedBreak(breakItem);
        setResolutionNote('');
        setModalError('');
    };

    const closeModal = () => {
        setSelectedBreak(null);
        setResolutionNote('');
        setModalError('');
        setIsSaving(false);
    };

    const confirmResolve = async () => {
        if (!selectedBreak) return;

        if (resolutionNote.trim().length < 5) {
            setModalError('Please enter at least 5 characters.');
            return;
        }

        setOptimistic(prev => ({ ...prev, [selectedBreak.id]: 'RESOLVED' }));
        setIsSaving(true);
        setModalError('');

        try {
            await resolveBreak(selectedBreak.id);
            closeModal();
            refetch();
        } catch (e) {
            setOptimistic(prev => {
                const next = { ...prev };
                delete next[selectedBreak.id];
                return next;
            });
            setModalError('Resolve failed: ' + (e.message || 'Please try again.'));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <>
            <h1>Reconciliation Breaks</h1>

            <div className="filters">
                {['OPEN', 'RESOLVED', 'SUPPRESSED'].map(s => (
                    <button key={s}
                            className={filter === s ? 'active' : ''}
                            onClick={() => setFilter(s)}>
                        {s}
                    </button>
                ))}
            </div>

            {loading && <div className="loading">Loading…</div>}
            {error   && <div className="error">{error.message}</div>}

            <table className="data-table">
                <thead>
                    <tr>
                        <th>Trade Ref</th>
                        <th>Discrepancy</th>
                        <th>Status</th>
                        <th>Detected</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    {results.map(r => {
                        const status = optimistic[r.id] || r.status;
                        const detected = r.detectedAt
                            ? new Date(r.detectedAt).toLocaleString('en-GB')
                            : '—';
                        return (
                            <tr key={r.id}>
                                <td>{r.tradeRef ?? r.tradeId ?? '—'}</td>
                                <td>{r.discrepancyType ?? '—'}</td>
                                <td><StatusBadge status={status} /></td>
                                <td>{detected}</td>
                                <td>
                                    {status === 'OPEN' && (
                                        <button onClick={() => openModal(r)}>
                                            Resolve
                                        </button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            <ResolveBreakModal
                open={Boolean(selectedBreak)}
                breakItem={selectedBreak}
                note={resolutionNote}
                onNoteChange={setResolutionNote}
                onClose={closeModal}
                onConfirm={confirmResolve}
                error={modalError}
                isSaving={isSaving}
            />
        </>
    );
}