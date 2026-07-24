import { useEffect, useMemo, useState } from 'react'
import {
  assetUrl,
  completeSale,
  createQuickCustomer,
  getPosProducts,
  lookupBarcode,
  searchCustomers,
  searchProducts,
} from '../../api'
import { money } from '../../utils/format'

export default function CashierCheckout({ onReceiptOpen }) {
  const [query, setQuery] = useState('')
  const [products, setProducts] = useState([])
  const [productsLoading, setProductsLoading] = useState(true)
  const [cart, setCart] = useState([])
  const [selectedCustomer, setSelectedCustomer] = useState(null)
  const [customerQuery, setCustomerQuery] = useState('')
  const [customerResults, setCustomerResults] = useState([])
  const [quickCustomer, setQuickCustomer] = useState({ fullName: '', phone: '' })
  const [showQuickCustomer, setShowQuickCustomer] = useState(false)
  const [selectedCategory, setSelectedCategory] = useState('All Products')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [amountReceived, setAmountReceived] = useState('')
  const [referenceNumber, setReferenceNumber] = useState('')
  const [completingSale, setCompletingSale] = useState(false)
  const [status, setStatus] = useState('')

  const subtotal = useMemo(
    () => cart.reduce((total, item) => total + Number(item.sellingPrice) * item.quantity, 0),
    [cart],
  )
  const changeDue = Math.max(Number(amountReceived || 0) - subtotal, 0)
  const isCashPayment = paymentMethod === 'CASH'
  const categoryOptions = useMemo(() => {
    const uniqueCategories = products
      .map((product) => product.categoryName)
      .filter(Boolean)
      .filter((category, index, list) => list.indexOf(category) === index)

    return ['All Products', ...uniqueCategories]
  }, [products])
  const visibleProducts = useMemo(() => {
    if (selectedCategory === 'All Products') {
      return products
    }

    return products.filter((product) => product.categoryName === selectedCategory)
  }, [products, selectedCategory])
  const cartItemCount = cart.reduce((total, item) => total + item.quantity, 0)

  useEffect(() => {
    loadProducts()
  }, [])

  async function loadProducts() {
    setProductsLoading(true)
    setStatus('')

    try {
      const data = await getPosProducts()
      setProducts(data)
    } catch (apiError) {
      setStatus(apiError.message)
      setProducts([])
    } finally {
      setProductsLoading(false)
    }
  }

  async function handleSearch(event) {
    event.preventDefault()
    setStatus('')

    const trimmedQuery = query.trim()

    if (!trimmedQuery) {
      await loadProducts()
      return
    }

    setProductsLoading(true)
    try {
      try {
        const barcodeProduct = await lookupBarcode(trimmedQuery)
        setProducts([barcodeProduct])
        addToCart(barcodeProduct)
        setQuery('')
        setStatus(`${barcodeProduct.name} added to cart.`)
        return
      } catch {
        // If it is not an exact barcode, continue with normal product search.
      }

      const data = await searchProducts(trimmedQuery)
      setProducts(data.length ? data : [])
    } catch (apiError) {
      setStatus(apiError.message)
      setProducts([])
    } finally {
      setProductsLoading(false)
    }
  }

  async function handleCustomerSearch(event) {
    event.preventDefault()
    setStatus('')

    if (!customerQuery.trim()) {
      setCustomerResults([])
      return
    }

    try {
      const data = await searchCustomers(customerQuery.trim())
      setCustomerResults(data)
    } catch (apiError) {
      setStatus(apiError.message)
    }
  }

  async function handleQuickCustomer(event) {
    event.preventDefault()
    setStatus('')

    if (!quickCustomer.fullName.trim() || !quickCustomer.phone.trim()) {
      setStatus('Customer name and phone are required.')
      return
    }

    try {
      const customer = await createQuickCustomer({
        fullName: quickCustomer.fullName.trim(),
        phone: quickCustomer.phone.trim(),
      })
      setSelectedCustomer(customer)
      setCustomerQuery('')
      setCustomerResults([])
      setQuickCustomer({ fullName: '', phone: '' })
      setShowQuickCustomer(false)
      setStatus(`${customer.fullName} selected.`)
    } catch (apiError) {
      setStatus(apiError.message)
    }
  }

  function addToCart(product) {
    const availableStock = Number(product.currentStock || 0)

    if (availableStock <= 0) {
      setStatus(`${product.name} is out of stock.`)
      return
    }

    setCart((currentCart) => {
      const existing = currentCart.find((item) => item.id === product.id)
      if (existing) {
        if (existing.quantity + 1 > availableStock) {
          setStatus(`Only ${money(availableStock)} in stock for ${product.name}.`)
          return currentCart
        }

        return currentCart.map((item) =>
          item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
        )
      }
      return [...currentCart, { ...product, quantity: 1 }]
    })
  }

  function updateQuantity(productId, quantity) {
    setCart((currentCart) =>
      currentCart
        .map((item) => {
          if (item.id !== productId) {
            return item
          }

          const availableStock = Number(item.currentStock || 0)
          const nextQuantity = Math.min(quantity, availableStock)
          if (quantity > availableStock) {
            setStatus(`Only ${money(availableStock)} in stock for ${item.name}.`)
          }
          return { ...item, quantity: nextQuantity }
        })
        .filter((item) => item.quantity > 0),
    )
  }

  function incrementQuantity(productId) {
    const item = cart.find((cartItem) => cartItem.id === productId)
    if (item) {
      updateQuantity(productId, item.quantity + 1)
    }
  }

  function decrementQuantity(productId) {
    const item = cart.find((cartItem) => cartItem.id === productId)
    if (item) {
      updateQuantity(productId, item.quantity - 1)
    }
  }

  function removeFromCart(productId) {
    setCart((currentCart) => currentCart.filter((item) => item.id !== productId))
  }

  async function handleCompleteSale() {
    setStatus('')

    if (!cart.length) {
      setStatus('Cart is empty.')
      return
    }

    const received = isCashPayment ? Number(amountReceived || 0) : subtotal
    if (received < subtotal) {
      setStatus('Amount received is less than total.')
      return
    }

    if (!isCashPayment && !referenceNumber.trim()) {
      setStatus('Reference number is required for non-cash payments.')
      return
    }

    setCompletingSale(true)
    try {
      const completedSale = await completeSale({
        customerId: selectedCustomer?.id || null,
        items: cart.map((item) => ({
          productId: item.id,
          quantity: item.quantity.toFixed(3),
        })),
        payment: {
          paymentMethod,
          amountReceived: received.toFixed(2),
          referenceNumber: referenceNumber.trim(),
        },
      })
      setCart([])
      setAmountReceived('')
      setReferenceNumber('')
      setSelectedCustomer(null)
      setStatus('Sale completed.')
      await loadProducts()
      onReceiptOpen(completedSale.saleId)
    } catch (apiError) {
      setStatus(apiError.message)
    } finally {
      setCompletingSale(false)
    }
  }

  return (
    <div className="cashier-checkout">
      <section className="cashier-catalog">
        <section className="pos-card products-card">
          <div className="cashier-product-tools">
            <form className="pos-search-bar cashier-product-search" onSubmit={handleSearch}>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search by product name, barcode or SKU..."
              />
              <button type="submit">Scan</button>
            </form>

            <div className="cashier-category-row">
              <div className="cashier-category-tabs">
                {categoryOptions.map((category) => (
                  <button
                    className={selectedCategory === category ? 'active' : ''}
                    key={category}
                    type="button"
                    onClick={() => setSelectedCategory(category)}
                  >
                    {category}
                  </button>
                ))}
              </div>
              <button className="filter-button" type="button" onClick={loadProducts} disabled={productsLoading}>
                Filter
              </button>
            </div>
          </div>

          <div className="product-grid">
            {productsLoading ? (
              <p className="empty-state">Loading real products...</p>
            ) : visibleProducts.length ? (
              visibleProducts.map((product) => {
                const stock = Number(product.currentStock || 0)
                const lowStock = stock > 0 && stock <= 5
                const outOfStock = stock <= 0

                return (
                  <article
                    className="product-tile"
                    key={product.id}
                  >
                    <button className="favorite-button" type="button" aria-label="Favorite product">*</button>
                    <div className="product-tile-image">
                      {product.imageUrl ? (
                        <img src={assetUrl(product.imageUrl)} alt={product.name} />
                      ) : (
                        <span>No image</span>
                      )}
                    </div>
                    <div className="product-tile-body">
                      <strong>{product.name}</strong>
                      <em>Rs {money(product.sellingPrice)}</em>
                      <div className="stock-line">
                        <span>In Stock: {money(product.currentStock)}</span>
                        {outOfStock ? <mark>Out</mark> : lowStock ? <mark>Low Stock</mark> : null}
                      </div>
                      <button type="button" onClick={() => addToCart(product)} disabled={outOfStock}>
                        Add
                      </button>
                    </div>
                  </article>
                )
              })
            ) : (
              <p className="empty-state">No products found.</p>
            )}
          </div>
        </section>

        <section className="pos-card customer-panel">
          <div className="panel-heading">
            <h2>Customer</h2>
            <button type="button" onClick={() => setShowQuickCustomer((current) => !current)}>
              New Customer
            </button>
          </div>
          <div className="customer-strip">
            <div>
              <span>Customer</span>
              <strong>{selectedCustomer?.fullName || 'Walk-in customer'}</strong>
              {selectedCustomer?.phone ? <small>{selectedCustomer.phone}</small> : null}
            </div>
            <div className="action-row">
              {selectedCustomer ? (
                <button type="button" onClick={() => setSelectedCustomer(null)}>Walk-in</button>
              ) : null}
            </div>
          </div>

          <form className="customer-search" onSubmit={handleCustomerSearch}>
            <input
              value={customerQuery}
              onChange={(event) => setCustomerQuery(event.target.value)}
              placeholder="Search customer by name or phone"
            />
            <button type="submit">Find</button>
          </form>

          {customerResults.length ? (
            <div className="customer-results">
              {customerResults.map((customer) => (
                <button
                  key={customer.id}
                  type="button"
                  onClick={() => {
                    setSelectedCustomer(customer)
                    setCustomerResults([])
                    setCustomerQuery('')
                  }}
                >
                  <strong>{customer.fullName}</strong>
                  <span>{customer.phone}</span>
                </button>
              ))}
            </div>
          ) : null}

          {showQuickCustomer ? (
            <form className="quick-customer-form" onSubmit={handleQuickCustomer}>
              <input
                value={quickCustomer.fullName}
                onChange={(event) => setQuickCustomer({ ...quickCustomer, fullName: event.target.value })}
                placeholder="Customer name"
              />
              <input
                value={quickCustomer.phone}
                onChange={(event) => setQuickCustomer({ ...quickCustomer, phone: event.target.value })}
                placeholder="Phone"
              />
              <button type="submit">Create</button>
            </form>
          ) : null}
        </section>
      </section>

      <aside className="cashier-cart-column">
        <section className="pos-card cart-card">
          <div className="panel-heading">
            <h2>Cart ({cartItemCount} Items)</h2>
            <button className="danger-button" type="button" onClick={() => setCart([])}>Clear Cart</button>
          </div>

          <div className="cart-table">
            <div className="cart-table-head">
              <span>Item</span>
              <span>Price</span>
              <span>Qty</span>
              <span>Total</span>
              <span></span>
            </div>
            {cart.length ? (
              cart.map((item) => (
                <div className="cart-table-row" key={item.id}>
                  <div className="cart-item-cell">
                    <span className="cart-product-image">
                      {item.imageUrl ? (
                        <img src={assetUrl(item.imageUrl)} alt={item.name} />
                      ) : (
                        'No'
                      )}
                    </span>
                    <strong>{item.name}</strong>
                  </div>
                  <span>Rs {money(item.sellingPrice)}</span>
                  <div className="qty-stepper">
                    <button type="button" onClick={() => decrementQuantity(item.id)}>-</button>
                    <input
                      min="0"
                      step="1"
                      type="number"
                      value={item.quantity}
                      onChange={(event) => updateQuantity(item.id, Number(event.target.value))}
                    />
                    <button type="button" onClick={() => incrementQuantity(item.id)}>+</button>
                  </div>
                  <span>Rs {money(Number(item.sellingPrice) * item.quantity)}</span>
                  <button className="remove-button" type="button" onClick={() => removeFromCart(item.id)}>x</button>
                </div>
              ))
            ) : (
              <p className="empty-state">Cart is empty.</p>
            )}
          </div>

          <div className="cart-total-row">
            <span>Subtotal</span>
            <strong>Rs {money(subtotal)}</strong>
          </div>
          <div className="cart-total-row">
            <span>Discount</span>
            <strong>Rs 0.00</strong>
          </div>
          <div className="cart-total-row">
            <span>Tax (0%)</span>
            <strong>Rs 0.00</strong>
          </div>
          <div className="cart-total-row">
            <span>Grand Total</span>
            <strong>Rs {money(subtotal)}</strong>
          </div>
          <div className="payment-card">
          <h2>Payment Method</h2>
          <div className="payment-methods">
            {['CASH', 'CARD', 'BANK_TRANSFER', 'MOBILE_WALLET'].map((method) => (
              <button
                className={paymentMethod === method ? 'active' : ''}
                key={method}
                type="button"
                onClick={() => {
                  setPaymentMethod(method)
                  setReferenceNumber('')
                  if (method !== 'CASH') {
                    setAmountReceived('')
                  }
                }}
              >
                {method.replaceAll('_', ' ')}
              </button>
            ))}
          </div>

          <div className="checkout-summary">
          <div>
            <span>Total</span>
            <strong>Rs {money(subtotal)}</strong>
          </div>

          <label>
            Amount Received
            <input
              value={amountReceived}
              onChange={(event) => setAmountReceived(event.target.value)}
              placeholder={money(subtotal)}
              type="number"
              disabled={!isCashPayment}
            />
          </label>

          {!isCashPayment ? (
            <label>
              Reference Number
              <input
                value={referenceNumber}
                onChange={(event) => setReferenceNumber(event.target.value)}
                placeholder="Transaction reference"
              />
            </label>
          ) : null}

          <div>
            <span>Change Due</span>
            <strong>Rs {money(changeDue)}</strong>
          </div>

          {status ? <p className="form-error neutral">{status}</p> : null}

          <button
            className="primary-button"
            type="button"
            onClick={handleCompleteSale}
            disabled={completingSale || !cart.length}
          >
            {completingSale ? 'Completing...' : 'Complete Sale'}
          </button>
          </div>
          </div>
        </section>
      </aside>
    </div>
  )
}

