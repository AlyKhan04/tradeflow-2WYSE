import { useEffect, useRef } from 'react';

export default function ResolveBreakModal({
  open,
  breakItem,
  note,
  onNoteChange,
  onClose,
  onConfirm,
  error,
  isSaving
}) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    const handleCancel = (event) => {
      event.preventDefault();
      onClose();
    };

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    dialog.addEventListener('cancel', handleCancel);
    document.addEventListener('keydown', handleKeyDown);

    if (open && !dialog.open) {
      dialog.showModal();
    }

    if (!open && dialog.open) {
      dialog.close();
    }

    return () => {
      dialog.removeEventListener('cancel', handleCancel);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open, onClose]);

  const handleBackdropClick = (event) => {
    if (event.target === dialogRef.current) {
      onClose();
    }
  };

  if (!breakItem) return null;

  return (
    <dialog ref={dialogRef} className="resolve-break-modal" onClick={handleBackdropClick}>
      <form
        method="dialog"
        className="dialog-content"
        onSubmit={(event) => {
          event.preventDefault();
          onConfirm();
        }}
      >
        <h2>Resolve break</h2>

        <div className="field">
          <label>Trade reference</label>
          <div>{breakItem.tradeRef ?? breakItem.tradeId ?? '—'}</div>
        </div>

        <div className="field">
          <label>Discrepancy</label>
          <div>{breakItem.discrepancyType ?? '—'}</div>
        </div>

        <div className="field">
          <label htmlFor="resolution-note">Resolution note</label>
          <textarea
            id="resolution-note"
            value={note}
            onChange={(event) => onNoteChange(event.target.value)}
            minLength={5}
            rows={4}
            placeholder="Enter at least 5 characters"
            autoFocus
          />
          <small>Minimum 5 characters</small>
        </div>

        {error && <div className="modal-error">{error}</div>}

        <div className="dialog-actions">
          <button type="button" onClick={onClose} disabled={isSaving}>
            Cancel
          </button>
          <button type="submit" disabled={isSaving || note.trim().length < 5}>
            {isSaving ? 'Resolving…' : 'Confirm'}
          </button>
        </div>
      </form>
    </dialog>
  );
}