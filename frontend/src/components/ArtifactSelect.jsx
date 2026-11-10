import { useEffect, useState } from 'react'
import { api } from '../api'

export default function ArtifactSelect({ value, onChange }) {
  const [artifacts, setArtifacts] = useState([])
  const [query, setQuery] = useState('')
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newEco, setNewEco] = useState('MAVEN')
  const [error, setError] = useState(null)

  useEffect(() => {
    api.listArtifacts(query).then(setArtifacts).catch((e) => setError(e.message))
  }, [query])

  async function handleCreate() {
    if (!newName.trim()) return
    try {
      const created = await api.createArtifact({ name: newName.trim(), ecosystem: newEco })
      onChange(created.id)
      setCreating(false)
      setNewName('')
      // обновить список
      api.listArtifacts(query).then(setArtifacts)
    } catch (e) {
      setError(e.message)
    }
  }

  const selected = artifacts.find((a) => a.id === value)

  return (
    <div className="artifact-select">
      <div className="row">
        <input
          placeholder="Поиск артефакта..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="search-input"
        />
        <button type="button" onClick={() => setCreating(!creating)} className="btn-secondary">
          {creating ? 'Отмена' : '+ Новый'}
        </button>
      </div>

      {!creating && (
        <select value={value || ''} onChange={(e) => onChange(Number(e.target.value) || null)}>
          <option value="">— выберите артефакт —</option>
          {artifacts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name} ({a.ecosystem})
            </option>
          ))}
        </select>
      )}

      {creating && (
        <div className="create-artifact">
          <input
            placeholder="Имя артефакта (напр. tomcat-embed-core)"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
          />
          <select value={newEco} onChange={(e) => setNewEco(e.target.value)}>
            <option value="MAVEN">Maven</option>
            <option value="NPM">npm</option>
          </select>
          <button type="button" onClick={handleCreate} className="btn-primary">
            Создать и выбрать
          </button>
        </div>
      )}

      {selected && !creating && (
        <div className="selected-hint">
          Выбран: <strong>{selected.name}</strong> ({selected.ecosystem})
        </div>
      )}
      {error && <div className="error">{error}</div>}
    </div>
  )
}
