import { useEffect, useMemo, useState } from 'react'
import { assetUrl, getDashboardStats, getProducts } from '../../api'
import { fallbackStats } from '../../utils/demoData'
import { money } from '../../utils/format'

export default function AdminDashboard({ onNavigate }) {
  const [stats, setStats] = useState(fallbackStats)
  const [products, setProducts] = useState([])
  const [error, setError] = useState('')
  const [productQuery, setProductQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState('All')

  useEffect(() => {
    let ignore = false

    Promise.all([
      getDashboardStats(),
      getProducts(),
    ])
      .then(([dashboardStats, productRows]) => {
        if (!ignore) {
          setStats(dashboardStats)
          setProducts(productRows)
        }
      })
      .catch((apiError) => {
        if (!ignore) {
          setError(apiError.message)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  const averageSale = stats.todaySaleCount
    ? Number(stats.todaySalesTotal || 0) / Number(stats.todaySaleCount)
    : 0
  const categoryFilters = useMemo(() => {
    const categories = products
      .map((product) => product.categoryName)
      .filter(Boolean)
      .filter((category, index, allCategories) => allCategories.indexOf(category) === index)

    return ['All', ...categories]
  }, [products])

  const visibleProducts = useMemo(() => {
    const normalizedQuery = productQuery.trim().toLowerCase()

    return products
      .filter((product) => {
        const matchesCategory =
          activeCategory === 'All' || product.categoryName?.toLowerCase().includes(activeCategory.toLowerCase())
        const matchesQuery =
          !normalizedQuery ||
          [product.name, product.sku, product.barcode, product.categoryName]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(normalizedQuery))

        return matchesCategory && matchesQuery
      })
  }, [activeCategory, productQuery, products])

  const metricCards = [
    {
      accent: 'green',
      detail: 'Today from backend',
      icon: '$',
      label: 'Today Sales',
      value: `Rs ${money(stats.todaySalesTotal)}`,
    },
    {
      accent: 'blue',
      detail: 'Completed today',
      icon: '#',
      label: 'Transactions',
      value: stats.todaySaleCount || 0,
    },
    {
      accent: 'amber',
      detail: 'Based on today sales',
      icon: '~',
      label: 'Average Sale',
      value: `Rs ${money(averageSale)}`,
    },
    {
      accent: 'orange',
      detail: 'View items',
      icon: '!',
      label: 'Low Stock',
      value: stats.lowStockCount || 0,
      onClick: () => onNavigate('Inventory'),
    },
  ]

  return (
    <div className="admin-dashboard">
      {error ? <div className="notice">Backend: {error}</div> : null}

      <section className="dashboard-metrics">
        {metricCards.map((card) => (
          <article className={`dashboard-metric ${card.accent}`} key={card.label}>
            <div>
              <span>{card.label}</span>
              <strong>{card.value}</strong>
              {card.onClick ? (
                <button type="button" onClick={card.onClick}>{card.detail}</button>
              ) : (
                <small>{card.detail}</small>
              )}
            </div>
            <i>{card.icon}</i>
          </article>
        ))}
      </section>

      <section className="dashboard-card product-showcase">
        <div className="dashboard-search">
          <input
            value={productQuery}
            onChange={(event) => setProductQuery(event.target.value)}
            placeholder="Search product or barcode"
          />
          <span>Scan</span>
        </div>

        <div className="category-tabs">
          {categoryFilters.map((category) => (
            <button
              className={activeCategory === category ? 'active' : ''}
              key={category}
              type="button"
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>

        <div className="dashboard-products">
          {visibleProducts.length ? (
            visibleProducts.map((product) => (
              <button className="dashboard-product" key={product.id} type="button" onClick={() => onNavigate('Products')}>
                <span className="dashboard-product-image">
                  {product.imageUrl ? <img src={assetUrl(product.imageUrl)} alt={product.name} /> : <b>No image</b>}
                </span>
                <span>
                  <strong>{product.name}</strong>
                  <em>Rs {money(product.sellingPrice)}</em>
                  <small>In Stock: {money(product.currentStock)}</small>
                </span>
              </button>
            ))
          ) : (
            <p className="empty-state">No products found.</p>
          )}
        </div>
      </section>
    </div>
  )
}
