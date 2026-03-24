# API Specification

Base URL: `/api/v1`

## Authentication Tiers

| Tier | Routes | Auth Method |
|------|--------|-------------|
| Public | `/store/**` | None |
| Webhook | `/webhooks/**` | Signature verification |
| Plugin | `/plugin/**` | `X-API-Key` header |
| Admin | `/admin/**` | JWT Bearer token |

## Public Endpoints

### GET /store/products
Returns active products sorted by sort_order.

### POST /store/checkout
Creates a payment checkout session.

## Plugin Endpoints

### GET /plugin/orders/pending
Returns orders with status QUEUED for fulfillment.

### POST /plugin/orders/{orderId}/confirm
Confirms order fulfillment by the plugin.

## Admin Endpoints

### POST /admin/auth/login
Authenticates admin user, returns JWT access token.

### POST /admin/auth/refresh
Rotates refresh token, returns new access token.

### GET /admin/stats
Dashboard statistics (revenue, order counts, player counts).

### CRUD /admin/products
Full product management (GET, POST, PUT, PATCH).

### GET /admin/orders
Paginated order history with status filtering.

### POST /admin/orders/{id}/refund
Triggers payment refund and rank removal.

### GET /admin/players/search?q={query}
Search players by username.

## Webhook Endpoints

### POST /webhooks/stripe
Receives Stripe webhook events. Raw body required for signature verification.

### POST /webhooks/paypal
Receives PayPal webhook events.
