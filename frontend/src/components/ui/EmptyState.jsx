import React from 'react';
import { FileSearch } from 'lucide-react';

export default function EmptyState({ title, body, icon: Icon = FileSearch }) {
  return (
    <div className="empty">
      <Icon size={28} />
      <b>{title}</b>
      <p>{body}</p>
    </div>
  );
}
