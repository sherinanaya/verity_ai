import React, { useState } from 'react';
import { ShieldCheck, UploadCloud } from 'lucide-react';
import { analyzeDocument } from '../../api/client';

const MAX_BYTES = 10 * 1024 * 1024;

export default function UploadPanel({ onComplete }) {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');
  const [dragOver, setDragOver] = useState(false);

  function choose(next) {
    setError('');
    if (!next) {
      setFile(null);
      return;
    }
    const name = next.name.toLowerCase();
    const pdf = name.endsWith('.pdf') || next.type === 'application/pdf';
    if (!pdf) {
      setFile(null);
      setError('Only PDF files are supported.');
      return;
    }
    if (next.size > MAX_BYTES) {
      setFile(null);
      setError('File must be smaller than 10 MB.');
      return;
    }
    setFile(next);
  }

  async function submit(event) {
    event.preventDefault();
    if (!file || busy) return;
    setBusy(true);
    setProgress(0);
    setError('');
    try {
      const result = await analyzeDocument(file, setProgress);
      setFile(null);
      event.target.reset();
      await onComplete(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setProgress(0);
    }
  }

  return (
    <form onSubmit={submit}>
      <label
        className={`dropzone ${dragOver ? 'over' : ''} ${file ? 'ready' : ''}`}
        onDragOver={(event) => { event.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragOver(false);
          choose(event.dataTransfer.files?.[0]);
        }}
      >
        <input
          type="file"
          accept="application/pdf,.pdf"
          onChange={(event) => choose(event.target.files?.[0])}
        />
        <div className="upload-icon"><UploadCloud size={26} /></div>
        <b>{file ? file.name : 'Drop a policy PDF here'}</b>
        <span>{file ? `${Math.ceil(file.size / 1024)} KB · ready to analyze` : 'or click to browse · PDF up to 10 MB'}</span>
      </label>
      {busy && (
        <div className="progress" aria-live="polite">
          <div className="progress-track"><i style={{ width: `${Math.max(progress, 8)}%` }} /></div>
          <span>{progress < 100 ? `Uploading ${progress}%` : 'Analyzing document…'}</span>
        </div>
      )}
      {error && <p className="form-error">{error}</p>}
      <div className="upload-footer">
        <span><ShieldCheck size={14} /> Candidate findings only. Human review is required.</span>
        <button className="primary-btn" type="submit" disabled={!file || busy}>
          {busy ? 'Analyzing…' : 'Analyze document'}
        </button>
      </div>
    </form>
  );
}
