import { useEffect, useState } from 'react'
import { api } from '../api'
import { Link } from 'react-router-dom'

export default function TicketList() {
  const [data, setData] = useState({ content: [], totalPages: 0, number: 0 })
  const [filters, setFilters] = useState({ actual: '', ecosystem: '', artifact: '', cve: '' })
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState({ field: 'createdAt', dir: 'desc' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true)
    api.listTickets({
      ...filters,
      actual: filters.actual === '' ? undefined : filters.actual,
      page,
      size: 20,
      sort: sort.field,
      dir: sort.dir,
    })
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [filters, page, sort])

  function toggleSort(field) {
    setSort((s) =>
      s.field === field ? { field, dir: s.dir === 'asc' ? 'desc' : 'asc' } : { field, dir: 'asc' }
    )
    setPage(0)
  }

  async function handleToggleActual(ticket) {
    const next = ticket.actual === null ? true : ticket.actual === true ? false : null
    try {
      await api.patchActual(ticket.id, next)
      setData((d) => ({
        ...d,
        content: d.content.map((t) => (t.id === ticket.id ? { ...t, actual: next } : t)),
      }))
    } catch (e) {
      setError(e.message)
    }
  }

  function actualLabel(v) {
    if (v === null) return '⏳ не триажено'
    return v ? '🔴 актуальна' : '🟢 не актуальна'
  }

  return (
    <div>
      <div className="header-row">
        <h1>Уязвимости</h1>
        <Link to="/tickets/new" className="btn-primary">+ Добавить тикет</Link>
      </div>

      <div className="filters">
        <select value={filters.actual} onChange={(e) => { setFilters({ ...filters, actual: e.target.value }); setPage(0) }}>
          <option value="">Все актуальности</option>
          <option value="true">Актуальна</option>
          <option value="false">Не актуальна</option>
          <option value="__null__">Не триажено</option>
        </select>
        {/* Для «не триажено» бэкенд ждёт actual= (пусто). Обработаем в useEffect */}
        <select value={filters.ecosystem} onChange={(e) => { setFilters({ ...filters, ecosystem: e.target.value }); setPage(0) }}>
          <option value="">Все экосистемы</option>
          <option value="MAVEN">Maven</option>
          <option value="NPM">npm</option>
        </select>
        <input
          placeholder="Артефакт..."
          value={filters.artifact}
          onChange={(e) => { setFilters({ ...filters, artifact: e.target.value }); setPage(0) }}
        />
        <input
          placeholder="CVE..."
          value={filters.cve}
          onChange={(e) => { setFilters({ ...filters, cve: e.target.value }); setPage(0) }}
        />
      </div>

      {error && <div className="error">{error}</div>}
      {loading && <div className="loading">Загрузка...</div>}

      <table className="ticket-table">
        <thead>
          <tr>
            <th>Тикет</th>
            <th>Артефакт</th>
            <th>CVE</th>
            <th onClick={() => toggleSort('deadline')} className="sortable">
              Дедлайн {sort.field === 'deadline' ? (sort.dir === 'asc' ? '↑' : '↓') : ''}
            </th>
            <th onClick={() => toggleSort('actual')} className="sortable">
              Актуальна {sort.field === 'actual' ? (sort.dir === 'asc' ? '↑' : '↓') : ''}
            </th>
            <th>Комментарий</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {data.content.map((t) => (
            <tr key={t.id}>
              <td><a href={t.jiraUrl} target="_blank" rel="noreferrer">{t.jiraUrl.split('/').pop()}</a></td>
              <td>{t.artifact.name} <span className="eco">{t.artifact.ecosystem}</span></td>
              <td className="cve-cell">
                {t.vulnerabilities.map((v) => (
                  <span key={v.id} className="cve-badge" title={v.description || ''}>{v.cveId}</span>
                ))}
              </td>
              <td>{t.deadline || '—'}</td>
              <td>
                <button className="actual-btn" onClick={() => handleToggleActual(t)}>
                  {actualLabel(t.actual)}
                </button>
              </td>
              <td className="comment-cell">{t.comment || '—'}</td>
              <td><Link to={`/tickets/${t.id}`}>✏️</Link></td>
            </tr>
          ))}
          {data.content.length === 0 && (
            <tr><td colSpan={7} className="empty">Нет данных</td></tr>
          )}
        </tbody>
      </table>

      <div className="pagination">
        <button disabled={page === 0} onClick={() => setPage(page - 1)}>← Назад</button>
        <span>Стр. {page + 1} из {data.totalPages || 1}</span>
        <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Вперёд →</button>
      </div>
    </div>
  )
}
