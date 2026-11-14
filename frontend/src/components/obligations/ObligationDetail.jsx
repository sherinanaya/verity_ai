import React, { useState } from 'react';
import Modal from '../ui/Modal';
import { formatWhen, modeLabel, statusLabel, STATUSES } from '../../utils/format';

export default function ObligationDetail({ item, onClose, onStatus, onDelete }) {
  const [busy, setBusy] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');

  async function changeStatus(status) {
    setBusy(true);
    setError('');
    try {
      await onStatus(item.id, status);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    setBusy(true);
    setError('');
    try {
      await onDelete(item.id);
      onClose();
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <Modal
      title="Finding detail"
      subtitle="A candidate observation for human review. This is not a legal determination."
      onClose={onClose}
    >
      <div className="detail-kicker">
        <span className={`risk-pill ${item.riskLevel?.toLowerCase()}`}>{item.riskLevel}</span>
        <span className={`status-pill ${item.status?.toLowerCase()}`}>{statusLabel(item.status)}</span>
        <span className={`origin-pill ${item.inferred ? 'inferred' : 'stated'}`}>
          {item.inferred ? 'Interpretation' : 'Document statement'}
        </span>
      </div>
      <h3 className="detail-title">{item.requirement}</h3>
      <dl className="detail-grid">
        <div><dt>Category</dt><dd>{item.category || 'General Governance'}</dd></div>
        <div><dt>Source document</dt><dd>{item.sourceDocument || 'Not linked'}</dd></div>
        <div><dt>Source page</dt><dd>{item.sourcePage ? `Page ${item.sourcePage}` : 'Not specified'}</dd></div>
        <div><dt>Recorded</dt><dd>{formatWhen(item.createdAt)}</dd></div>
      </dl>
      <section className="detail-block">
        <h4>Evidence excerpt</h4>
        {item.sourceExcerpt
          ? <blockquote>{item.sourceExcerpt}</blockquote>
          : <p className="muted">No direct quote was verified in the document. Treat this finding as an interpretation.</p>}
      </section>
      <section className="detail-block">
        <h4>Why it was flagged</h4>
        <p>{item.rationale || 'Review the source policy before deciding whether this applies.'}</p>
      </section>
      <section className="detail-block">
        <h4>Suggested next action</h4>
        <p>{item.recommendation || 'Assign an owner and decide what evidence would show the control is in place.'}</p>
      </section>
      <label className="field">
        <span>Review status</span>
        <select value={item.status} disabled={busy} onChange={(event) => changeStatus(event.target.value)}>
          {STATUSES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      {error && <p className="form-error">{error}</p>}
      <div className="detail-actions">
        {confirming ? (
          <>
            <span>Delete this finding?</span>
            <button type="button" className="ghost-btn" onClick={() => setConfirming(false)} disabled={busy}>Cancel</button>
            <button type="button" className="danger-btn" onClick={remove} disabled={busy}>Delete</button>
          </>
        ) : (
          <button type="button" className="text-danger" onClick={() => setConfirming(true)}>Delete finding</button>
        )}
      </div>
      <p className="fine-print">{item.inferred ? 'Interpretation generated during analysis.' : 'Excerpt comes from the uploaded document.'} Analysis mode is recorded on the parent assessment{item.sourceDocument ? ` (${item.sourceDocument})` : ''}. Status labels such as {modeLabel('AI')} describe the extractor, not a compliance verdict.</p>
    </Modal>
  );
}
