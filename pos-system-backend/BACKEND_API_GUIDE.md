# POS System Backend API Guide

## Demo Users

- Admin: `admin` / `admin123`
- Cashier: `cashier` / `cashier123`

Use `POST /api/auth/login` to get a JWT token, then send:

```text
Authorization: Bearer <token>
```

## Main Backend Areas

- Authentication: `POST /api/auth/login`
- Dashboard: `GET /api/dashboard/stats`
- Categories: `/api/categories`
- Products: `/api/products`
- POS product lookup: `/api/pos/products`
- Customers: `/api/customers`
- Inventory: `/api/inventory`
- Sales: `/api/sales`
- Admin cashier users: `/api/admin/users/cashiers`
- Reports: `/api/reports`

## Reports

All report endpoints are admin-only and require `startDate` and `endDate`
query parameters in `YYYY-MM-DD` format.

- `GET /api/reports/sales?startDate=2026-07-01&endDate=2026-07-05`
- `GET /api/reports/product-sales?startDate=2026-07-01&endDate=2026-07-05`
- `GET /api/reports/inventory-movements?startDate=2026-07-01&endDate=2026-07-05`

Invalid date ranges return a consistent JSON error response.

## Error Response Shape

```json
{
  "timestamp": "2026-07-05T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/admin/users/cashiers",
  "validationErrors": {
    "username": "Username is required"
  }
}
```

## Audit Logs

The backend records audit logs for important actions:

- cashier user creation
- cashier status changes
- cashier password resets
- cashier profile updates
- manual stock adjustments
- sale cancellations

Audit logs are stored in the `audit_logs` table and include actor, action,
entity type, entity ID, message, and timestamp.

## Swagger

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

The OpenAPI configuration includes JWT bearer authentication globally.
