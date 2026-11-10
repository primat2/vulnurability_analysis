import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import ArtifactSelect from '../components/ArtifactSelect'

export default function TicketForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = Boolean(id)

  const [form, setForm] = useState({
    jiraUrl: '',
    artifactId: null,
    deadline: '',
    actual: null,
    comment: '',
    cveIdsText: '',
  })
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!isEdit) return
    api.getTicket(id)
      .then((t) =>
        setForm({
          jiraUrl: t.jiraUrl,
          artifactId: t.artifact.id,
          deadline: t.deadline || '',
          actual: t.actual,
          comment: t.comment || '',
          cveIdsText: t.vulnerabilities.map((v) => v.cveId).join(', '),
        })
      )
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.jiraUrl.trim() || !form.artifactId) {
      setError('Ссылка на тикет и артефакт обязательны')
      return
    }
    setSaving(true)
    setError(null)
    const body = {
      jiraUrl: form.jiraUrl.trim(),
      artifactId: form.artifactId,
      deadline: form.deadline || null,
      actual: form.actual,
      comment: form.comment.trim() || null,
      cveIds: form.cveIdsText
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    }
    try {
      if (isEdit) await api.updateTicket(id, body)
      else await api.createTicket(body)
      navigate('/')
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div>Загрузка...</div>

  return (
    <div>
      <h1>{isEdit ? 'Редактировать тикет' : 'Новый тикет'}</h1>
      {error && <div className="error">{error}</div>}

      <form onSubmit={handleSubmit} className="ticket-form">
        <label>
          Ссылка на задачу (Jira)
          <input
            value={form.jiraUrl}
            onChange={(e) => set('jiraUrl', e.target.value)}
            placeholder="https://ihelp.rt.ru/browse/ELKDEV-..."
            required
          />
        </label>

        <label>
          Артефакт
          <ArtifactSelect value={form.artifactId} onChange={(v) => set('artifactId', v)} />
        </label>

        <label>
          CVE (через запятую)
          <input
            value={form.cveIdsText}
            onChange={(e) => set('cveIdsText', e.target.value)}
            placeholder="CVE-2026-11111, CVE-2026-22222"
          />
        </label>

        <label>
          Дедлайн
          <input type="date" value={form.deadline} onChange={(e) => set('deadline', e.target.value)} />
        </label>

        <label>
          Актуальность
          <select
            value={form.actual === null ? '__null__' : String(form.actual)}
            onChange={(e) =>
              set('actual', e.target.value === '__null__' ? null : e.target.value === 'true')
            }
          >
            <option value="__null__">Не триажено</option>
            <option value="true">Актуальна</option>
            <option value="false">Не актуальна</option>
          </select>
        </label>

        <label>
          Комментарий
          <textarea
            rows={4}
            value={form.comment}
            onChange={(e) => set('comment', e.target.value)}
            placeholder="Почему актуальна / не актуальна..."
          />
        </label>

        <div className="form-actions">
          <button type="submit" disabled={saving} className="btn-primary">
            {saving ? 'Сохранение...' : 'Сохранить'}
          </button>
          <button type="button" onClick={() => navigate('/')} className="btn-secondary">Отмена</button>
        </div>
      </form>
    </div>
  )
}
