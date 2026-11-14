const base = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '');

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${base}${path}`, options);
  } catch {
    throw new Error('Cannot connect to the VerityAI API. Start the backend and check VITE_API_URL.');
  }
  const text = await response.text();
  const data = text ? parseJson(text) : null;
  if (!response.ok) {
    throw new Error(data?.message || data?.error || `Request failed (${response.status}).`);
  }
  return data;
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return { message: 'The API returned an unexpected response.' };
  }
}

function queryString(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value);
    }
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

export function analyzeDocument(file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${base}/analyze`);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.min(100, Math.round((event.loaded / event.total) * 100)));
      }
    };
    xhr.onload = () => {
      const data = parseJson(xhr.responseText || '');
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(data);
        return;
      }
      reject(new Error(data?.message || data?.error || 'Analysis failed.'));
    };
    xhr.onerror = () => reject(new Error('Cannot connect to the VerityAI API. Start the backend and check VITE_API_URL.'));
    const body = new FormData();
    body.append('file', file);
    xhr.send(body);
  });
}

export const api = {
  health: () => request('/health'),
  assessments: () => request('/assessments'),
  assessment: (id) => request(`/assessments/${id}`),
  assessmentObligations: (id) => request(`/assessments/${id}/obligations`),
  obligations: (params) => request(`/obligations${queryString(params)}`),
  obligation: (id) => request(`/obligations/${id}`),
  updateStatus: (id, status) => request(`/obligations/${id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  }),
  deleteObligation: (id) => request(`/obligations/${id}`, { method: 'DELETE' }),
  analyze: analyzeDocument,
};
