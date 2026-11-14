import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';

const WorkspaceContext = createContext(null);

export function WorkspaceProvider({ children }) {
  const [assessments, setAssessments] = useState([]);
  const [obligations, setObligations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [backendOk, setBackendOk] = useState(null);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      const [nextAssessments, nextObligations] = await Promise.all([
        api.assessments(),
        api.obligations(),
      ]);
      setAssessments(nextAssessments);
      setObligations(nextObligations);
      setBackendOk(true);
      setError('');
    } catch (err) {
      setBackendOk(false);
      setError(err.message || 'Cannot reach the VerityAI API.');
    } finally {
      setRefreshing(false);
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const updateStatus = useCallback(async (id, status) => {
    const updated = await api.updateStatus(id, status);
    setObligations((current) => current.map((item) => (item.id === id ? updated : item)));
    return updated;
  }, []);

  const removeObligation = useCallback(async (id) => {
    await api.deleteObligation(id);
    await refresh();
  }, [refresh]);

  const value = useMemo(() => ({
    assessments,
    obligations,
    loading,
    refreshing,
    error,
    backendOk,
    refresh,
    updateStatus,
    removeObligation,
    clearError: () => setError(''),
  }), [assessments, obligations, loading, refreshing, error, backendOk, refresh, updateStatus, removeObligation]);

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const value = useContext(WorkspaceContext);
  if (!value) {
    throw new Error('useWorkspace must be used within WorkspaceProvider');
  }
  return value;
}
