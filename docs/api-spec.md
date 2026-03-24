# API Specification

Base URL: `/api/v1`

## Authentication Tiers

| Tier | Routes | Auth Method |
|------|--------|-------------|
| Public | `/store/**` | None |
| Webhook | `/webhooks/**` | Stripe/PayPal signature verification |
| Plugin | `/plugin/**` | `X-API-Key` + `X-Server-Id` headers |
| Admin | `/admin/**` | JWT Bearer token (15-min expiry) |

---

## Public Endpoints

### GET /store/products

Returns active products sorted by `sort_order`.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "slug": "vip",
    "name": "VIP Rank",
    "description": "Access to VIP perks and commands",
    "priceCents": 999,
    "rankGroup": "vip",
    "category": "ranks",
    "imageUrl": "https://example.com/vip.png",
    "active": true,
    "sortOrder": 1
  }
]
```

### POST /store/checkout

Creates a Stripe Checkout Session and returns the redirect URL.

**Request:**
```json
{
  "productSlug": "vip",
  "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "playerName": "Notch",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```json
{
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_..."
}
```

**Errors:**
- `404` — Product not found or inactive
- `409` — Duplicate idempotency key (order already exists)

---

## Plugin Endpoints

All plugin endpoints require:
- `X-API-Key: <PLUGIN_API_KEY>` header
- `X-Server-Id: <server-id>` header (optional, for multi-server tracking)

### GET /plugin/orders/pending

Returns orders with status `QUEUED` awaiting fulfillment.

**Response:** `200 OK`
```json
[
  {
    "orderId": "42",
    "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
    "productSlug": "vip",
    "rankGroup": "vip",
    "action": "GRANT_RANK",
    "amountCents": 999
  }
]
```

### POST /plugin/orders/{orderId}/confirm

Confirms order fulfillment. Transitions order status from `QUEUED` to `FULFILLED`.

**Request:**
```json
{
  "serverId": "survival-1"
}
```

**Response:** `200 OK`

**Errors:**
- `404` — Order not found
- `409` — Order not in QUEUED status

### POST /plugin/players/join

Reports a player join event to the backend.

**Request:**
```json
{
  "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "playerName": "Notch"
}
```

**Response:** `200 OK`

---

## Admin Endpoints

All admin endpoints (except auth) require: `Authorization: Bearer <access_token>`

### POST /admin/auth/login

**Request:**
```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900
}
```
Also sets `refresh_token` HttpOnly cookie (7-day expiry).

**Default credentials:** `admin` / `admin` (created on first startup — change immediately).

### POST /admin/auth/refresh

Reads `refresh_token` cookie, rotates it, returns new access token.

**Response:** `200 OK` — Same format as login.

### POST /admin/auth/logout

Revokes the refresh token. Clears the cookie.

### GET /admin/stats

**Response:** `200 OK`
```json
{
  "totalRevenueCents": 149850,
  "totalOrders": 87,
  "totalPlayers": 42,
  "pendingOrders": 3
}
```

### GET /admin/products

Returns all products (including inactive).

### POST /admin/products

**Request:**
```json
{
  "name": "VIP Rank",
  "slug": "vip",
  "priceCents": 999,
  "description": "Access to VIP perks",
  "rankGroup": "vip",
  "category": "ranks",
  "imageUrl": "https://example.com/vip.png"
}
```

**Response:** `201 Created` — Returns created product.

### PUT /admin/products/{id}

Updates a product. Same request body as create (all fields optional).

### PATCH /admin/products/{id}/active

**Request:**
```json
{
  "active": false
}
```

### GET /admin/orders?page=0&size=20&status=FULFILLED

Paginated order history with optional status filter.

**Response:** `200 OK`
```json
{
  "content": [{ "id": 1, "playerName": "Notch", "..." : "..." }],
  "totalElements": 87,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

### GET /admin/orders/{id}

Single order detail.

### POST /admin/orders/{id}/refund

Triggers Stripe refund and queues rank removal. Only works for `PAID`, `QUEUED`, or `FULFILLED` orders.

### GET /admin/players?q=Notch

Search players by username (minimum 2 characters).

### GET /admin/players/{uuid}

Player detail by Minecraft UUID.

### GET /admin/players/{uuid}/orders

Order history for a specific player.

---

## Webhook Endpoints

### POST /webhooks/stripe

Receives Stripe webhook events. Accepts **raw request body** (not JSON-parsed) for signature verification.

**Required header:** `Stripe-Signature`

**Handled events:**
- `checkout.session.completed` — Creates/updates order, transitions to QUEUED
- `charge.refunded` — Marks order REFUNDED, queues rank removal

**Response:** Always `200 OK` (even on processing errors, to prevent Stripe retries).

### POST /webhooks/paypal

Receives PayPal webhook events (stub — not yet implemented).

---

## Error Response Format

All errors follow this format:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: vip-plus",
  "timestamp": "2026-03-24T22:30:00Z",
  "fieldErrors": null
}
```

Validation errors (400) include field-level details:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-03-24T22:30:00Z",
  "fieldErrors": {
    "name": "must not be blank",
    "priceCents": "must be at least 1"
  }
}
```

## Rate Limits

| Tier | Limit | Scope |
|------|-------|-------|
| Store | 60 req/min | Per IP |
| Webhooks | 100 req/min | Per IP |
| Plugin | 120 req/min | Per API key |
| Admin | 300 req/min | Per IP |

Exceeding returns `429 Too Many Requests`.

## Order Status Lifecycle

```
PENDING_PAYMENT → PAID → QUEUED → FULFILLED
                                 ↘ REFUNDED
                ↘ FAILED
```
