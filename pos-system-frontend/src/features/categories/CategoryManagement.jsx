import { useEffect, useState } from 'react'
import { createCategory, getCategories, updateCategory, updateCategoryStatus } from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { blankCategoryForm } from '../../utils/formDefaults'

export default function CategoryManagement() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(blankCategoryForm)
  const [editingCategory, setEditingCategory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const categoryPagination = usePagination(categories, 10, categories.length)

  useEffect(() => {
    loadCategoryData()
  }, [])

  async function loadCategoryData() {
    setLoading(true)
    setError('')

    try {
      const nextCategories = await getCategories()
      setCategories(nextCategories)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setForm(blankCategoryForm)
    setEditingCategory(null)
    setSuccess('')
    setError('')
  }

  function startEdit(category) {
    setEditingCategory(category)
    setSuccess('')
    setError('')
    setForm({
      name: category.name || '',
      description: category.description || '',
    })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!form.name.trim()) {
      setError('Category name is required.')
      return
    }

    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
    }

    setSaving(true)
    try {
      if (editingCategory) {
        await updateCategory(editingCategory.id, payload)
        setSuccess('Category updated.')
      } else {
        await createCategory(payload)
        setSuccess('Category created.')
      }
      setForm(blankCategoryForm)
      setEditingCategory(null)
      await loadCategoryData()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleStatusToggle(category) {
    setError('')
    setSuccess('')
    const nextStatus = category.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    try {
      await updateCategoryStatus(category.id, nextStatus)
      setSuccess(`${category.name} marked ${nextStatus.toLowerCase()}.`)
      await loadCategoryData()
    } catch (apiError) {
      setError(apiError.message)
    }
  }

  return (
    <div className="management-layout compact">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Catalog setup</p>
            <h2>Categories</h2>
          </div>
          <button type="button" onClick={loadCategoryData} disabled={loading}>Refresh</button>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4">Loading categories...</td>
                </tr>
              ) : categories.length ? (
                categoryPagination.pageItems.map((category) => (
                  <tr key={category.id}>
                    <td><strong>{category.name}</strong></td>
                    <td>{category.description || 'No description'}</td>
                    <td><StatusPill status={category.status} /></td>
                    <td>
                      <div className="action-row">
                        <button type="button" onClick={() => startEdit(category)}>Edit</button>
                        <button type="button" onClick={() => handleStatusToggle(category)}>
                          {category.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4">No categories found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...categoryPagination} />
      </section>

      <aside className="form-panel">
        <div className="panel-heading">
          <h2>{editingCategory ? 'Edit Category' : 'Add Category'}</h2>
          {editingCategory ? <button type="button" onClick={resetForm}>Cancel</button> : null}
        </div>

        <form className="management-form" onSubmit={handleSubmit}>
          <label>
            Category Name
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>

          <label>
            Description
            <textarea
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
          </label>

          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? 'Saving...' : editingCategory ? 'Save Changes' : 'Create Category'}
          </button>
        </form>
      </aside>
    </div>
  )
}

