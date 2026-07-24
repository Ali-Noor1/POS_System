import { useEffect, useMemo, useState } from 'react'
import {
  assetUrl,
  createProduct,
  getCategories,
  getProducts,
  updateProduct,
  updateProductStatus,
  uploadProductImage,
} from '../../api'
import PaginationControls from '../../components/PaginationControls'
import StatusPill from '../../components/StatusPill'
import { usePagination } from '../../hooks/usePagination'
import { blankProductForm } from '../../utils/formDefaults'
import { money } from '../../utils/format'

export default function ProductManagement() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(blankProductForm)
  const [editingProduct, setEditingProduct] = useState(null)
  const [imageFile, setImageFile] = useState(null)
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const selectedImagePreview = useMemo(
    () => (imageFile ? URL.createObjectURL(imageFile) : ''),
    [imageFile],
  )
  const imagePreview = selectedImagePreview || (editingProduct?.imageUrl ? assetUrl(editingProduct.imageUrl) : '')

  useEffect(() => {
    loadProductData()
  }, [])

  useEffect(() => {
    return () => {
      if (selectedImagePreview) {
        URL.revokeObjectURL(selectedImagePreview)
      }
    }
  }, [selectedImagePreview])

  async function loadProductData() {
    setLoading(true)
    setError('')

    try {
      const [nextProducts, nextCategories] = await Promise.all([
        getProducts(),
        getCategories(),
      ])
      setProducts(nextProducts)
      setCategories(nextCategories)
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setLoading(false)
    }
  }

  const filterText = filter.trim().toLowerCase()
  const filteredProducts = useMemo(
    () =>
      products.filter((product) => {
        const haystack = [
          product.name,
          product.sku,
          product.barcode,
          product.brand,
          product.categoryName,
          product.status,
        ].join(' ').toLowerCase()
        return haystack.includes(filterText)
      }),
    [filterText, products],
  )
  const productPagination = usePagination(filteredProducts, 10, filterText)

  function updateField(field, value) {
    setForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  function resetForm() {
    setForm(blankProductForm)
    setEditingProduct(null)
    setImageFile(null)
    setSuccess('')
    setError('')
  }

  function startEdit(product) {
    setEditingProduct(product)
    setImageFile(null)
    setSuccess('')
    setError('')
    setForm({
      categoryId: String(product.categoryId || ''),
      name: product.name || '',
      sku: product.sku || '',
      barcode: product.barcode || '',
      brand: product.brand || '',
      description: product.description || '',
      costPrice: String(product.costPrice ?? ''),
      sellingPrice: String(product.sellingPrice ?? ''),
      reorderLevel: String(product.reorderLevel ?? '0'),
    })
  }

  function handleImageChange(event) {
    const file = event.target.files?.[0] || null

    if (!file) {
      setImageFile(null)
      return
    }

    if (!file.type.startsWith('image/')) {
      setError('Please choose a valid image file.')
      event.target.value = ''
      return
    }

    setError('')
    setImageFile(file)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!form.categoryId || !form.name.trim() || !form.sku.trim()) {
      setError('Category, product name, and SKU are required.')
      return
    }

    if (!form.costPrice || !form.sellingPrice || !form.reorderLevel) {
      setError('Cost price, selling price, and reorder level are required.')
      return
    }

    if (Number(form.costPrice) < 0 || Number(form.sellingPrice) < 0 || Number(form.reorderLevel) < 0) {
      setError('Prices and reorder level cannot be negative.')
      return
    }

    const payload = {
      categoryId: Number(form.categoryId),
      name: form.name.trim(),
      sku: form.sku.trim(),
      barcode: form.barcode.trim() || null,
      brand: form.brand.trim() || null,
      description: form.description.trim() || null,
      costPrice: form.costPrice,
      sellingPrice: form.sellingPrice,
      reorderLevel: form.reorderLevel,
    }

    setSaving(true)
    try {
      let savedProduct
      if (editingProduct) {
        savedProduct = await updateProduct(editingProduct.id, payload)
      } else {
        savedProduct = await createProduct(payload)
      }

      if (imageFile) {
        await uploadProductImage(savedProduct.id, imageFile)
      }

      setSuccess(imageFile
        ? `Product ${editingProduct ? 'updated' : 'created'} with image.`
        : `Product ${editingProduct ? 'updated' : 'created'}.`)
      setForm(blankProductForm)
      setEditingProduct(null)
      setImageFile(null)
      await loadProductData()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleStatusToggle(product) {
    setError('')
    setSuccess('')
    const nextStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    try {
      await updateProductStatus(product.id, nextStatus)
      setSuccess(`${product.name} marked ${nextStatus.toLowerCase()}.`)
      await loadProductData()
    } catch (apiError) {
      setError(apiError.message)
    }
  }

  return (
    <div className="management-layout product-management-layout">
      <section className="data-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Catalog</p>
            <h2>Products</h2>
          </div>
          <button type="button" onClick={loadProductData} disabled={loading}>Refresh</button>
        </div>

        <div className="table-toolbar">
          <input
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            placeholder="Search name, SKU, barcode, category"
          />
          <span>{filteredProducts.length} items</span>
        </div>

        {error ? <p className="form-error">{error}</p> : null}
        {success ? <p className="form-success">{success}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Image</th>
                <th>Product</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7">Loading products...</td>
                </tr>
              ) : filteredProducts.length ? (
                productPagination.pageItems.map((product) => (
                  <tr key={product.id}>
                    <td>
                      {product.imageUrl ? (
                        <img className="product-thumb" src={assetUrl(product.imageUrl)} alt={product.name} />
                      ) : (
                        <span className="product-thumb placeholder">No image</span>
                      )}
                    </td>
                    <td>
                      <strong>{product.name}</strong>
                      <span>{product.sku}{product.barcode ? ` / ${product.barcode}` : ''}</span>
                    </td>
                    <td>{product.categoryName || 'Unassigned'}</td>
                    <td>Rs {money(product.sellingPrice)}</td>
                    <td>{money(product.currentStock)}</td>
                    <td><StatusPill status={product.status} /></td>
                    <td>
                      <div className="action-row">
                        <button type="button" onClick={() => startEdit(product)}>Edit</button>
                        <button type="button" onClick={() => handleStatusToggle(product)}>
                          {product.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7">No products found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls {...productPagination} />
      </section>

      <aside className="form-panel">
        <div className="panel-heading">
          <h2>{editingProduct ? 'Edit Product' : 'Add Product'}</h2>
          {editingProduct ? <button type="button" onClick={resetForm}>Cancel</button> : null}
        </div>

        <form className="management-form" onSubmit={handleSubmit}>
          <label>
            Category
            <select value={form.categoryId} onChange={(event) => updateField('categoryId', event.target.value)}>
              <option value="">Select category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}{category.status === 'INACTIVE' ? ' (Inactive)' : ''}
                </option>
              ))}
            </select>
          </label>

          <label>
            Product Name
            <input value={form.name} onChange={(event) => updateField('name', event.target.value)} />
          </label>

          <div className="form-grid-two">
            <label>
              SKU
              <input value={form.sku} onChange={(event) => updateField('sku', event.target.value)} />
            </label>
            <label>
              Barcode
              <input value={form.barcode} onChange={(event) => updateField('barcode', event.target.value)} />
            </label>
          </div>

          <label>
            Brand
            <input value={form.brand} onChange={(event) => updateField('brand', event.target.value)} />
          </label>

          <label>
            Product Image
            <input accept="image/*" type="file" onChange={handleImageChange} />
          </label>

          {imagePreview ? (
            <div className="image-preview">
              <img src={imagePreview} alt="Selected product" />
              <span>{imageFile ? imageFile.name : 'Current image'}</span>
            </div>
          ) : null}

          <div className="form-grid-two">
            <label>
              Cost Price
              <input
                min="0"
                step="0.01"
                type="number"
                value={form.costPrice}
                onChange={(event) => updateField('costPrice', event.target.value)}
              />
            </label>
            <label>
              Selling Price
              <input
                min="0"
                step="0.01"
                type="number"
                value={form.sellingPrice}
                onChange={(event) => updateField('sellingPrice', event.target.value)}
              />
            </label>
          </div>

          <label>
            Reorder Level
            <input
              min="0"
              step="0.001"
              type="number"
              value={form.reorderLevel}
              onChange={(event) => updateField('reorderLevel', event.target.value)}
            />
          </label>

          <label>
            Description
            <textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} />
          </label>

          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? 'Saving...' : editingProduct ? 'Save Changes' : 'Create Product'}
          </button>
        </form>
      </aside>
    </div>
  )
}

