const BASE = '/api'

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!res.ok) {
    const problem = await res.json().catch(() => ({ detail: res.statusText }))
    throw new Error(problem.detail || `HTTP ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  // Tickets
  listTickets(params = {}) {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') q.append(k, v)
    })
    return request(`/tickets?${q}`)
  },
  getTicket: (id) => request(`/tickets/${id}`),
  createTicket: (body) => request('/tickets', { method: 'POST', body: JSON.stringify(body) }),
  updateTicket: (id, body) => request(`/tickets/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  patchActual: (id, actual) =>
    request(`/tickets/${id}/actual`, { method: 'PATCH', body: JSON.stringify({ actual }) }),
  deleteTicket: (id) => request(`/tickets/${id}`, { method: 'DELETE' }),

  // Artifacts
  listArtifacts: (q = '') => request(`/artifacts?q=${encodeURIComponent(q)}`),
  createArtifact: (body) => request('/artifacts', { method: 'POST', body: JSON.stringify(body) }),

  // Vulnerabilities (для страницы CVE, опционально)
  listVulnerabilities: (q = '') => request(`/vulnerabilities?q=${encodeURIComponent(q)}`),
  patchDescription: (id, description) =>
    request(`/vulnerabilities/${id}/description`, { method: 'PATCH', body: JSON.stringify({ description }) }),
}
