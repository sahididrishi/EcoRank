# Architecture

## System Overview

EcoRank is a mono-repo with three decoupled modules communicating over HTTP:

```
┌─────────────────┐     HTTP/REST       ┌──────────────────────┐
│  Minecraft       │◄──────────────────►│  Spring Boot Backend  │
│  Plugin (Paper)  │  X-API-Key auth    │                      │
│                  │  Polls every 5-30s │  ┌──────────────┐    │
└─────────────────┘                    │  │ PostgreSQL 16 │    │
                                       │  └──────────────┘    │
┌─────────────────┐                    │  ┌──────────────┐    │
│  React SPA       │◄──────────────────►│  │   Redis 7    │    │
│  (Vite + TW)     │  JWT auth          │  └──────────────┘    │
└─────────────────┘                    └──────────────────────┘
         ▲                                        ▲
         │                                        │
    Players browse                         Stripe/PayPal
    store & checkout                       webhooks
```

The plugin and backend **do not share a database** — they communicate exclusively via REST API.

---

## Module Breakdown

### Plugin (`ecorank-plugin/`)

**Tech:** Paper 1.21.x, Java 21, Maven, HikariCP, OkHttp, Gson

**Internal structure follows a service-layer pattern:**

```
EcoRankPlugin.java          ← Thin entry point (wiring only)
├── config/ConfigService    ← Typed config wrapper, fail-fast validation
├── storage/
│   ├── StorageProvider     ← Interface (SQLite or MySQL)
│   ├── SqlStorageProvider  ← Abstract JDBC + HikariCP base
│   ├── SqliteStorage       ← SQLite (single-writer, WAL mode)
│   └── MysqlStorage        ← MySQL (connection pooling)
├── service/
│   ├── EconomyService      ← Balance CRUD, transfers, transactions
│   ├── RankService         ← LuckPerms grant/remove via CompletableFuture
│   └── FulfillmentService  ← Order processing, offline player queue
├── command/                ← Brigadier commands via LifecycleEventManager
├── listener/               ← Join, mob kill, chest quest, daily login
├── api/
│   ├── BackendClient       ← OkHttp async HTTP client
│   └── PlaceholderExpansion← PAPI placeholders
└── task/
    └── FulfillmentPollTask ← Self-scheduling with exponential backoff
```

**Critical threading rule:** HTTP calls run async (`runTaskAsynchronously`), but all Bukkit/LuckPerms API calls run on the **main server thread** (`runTask`).

### Backend (`ecorank-backend/`)

**Tech:** Spring Boot 3.2, PostgreSQL 16, Redis 7, Flyway, MapStruct, Stripe SDK

```
EcoRankBackendApplication.java
├── config/
│   ├── SecurityConfig      ← Three ordered SecurityFilterChain beans
│   ├── EcoRankProperties   ← @ConfigurationProperties binding
│   ├── RedisConfig         ← RedisTemplate + graceful degradation
│   ├── RateLimitConfig     ← Bucket4j per-IP rate limiting
│   └── CorsConfig          ← Profile-dependent CORS
├── security/
│   ├── JwtUtil             ← HMAC-SHA256 token generation/validation
│   ├── JwtAuthFilter       ← Bearer token extraction for admin routes
│   └── ApiKeyFilter        ← X-API-Key check for plugin routes
├── entity/                 ← JPA entities (never exposed in API)
├── dto/                    ← Request/response records (MapStruct mapped)
├── mapper/                 ← MapStruct entity ↔ DTO converters
├── repository/             ← Spring Data JPA with custom queries
├── service/
│   ├── OrderService        ← Order state machine, price snapshot
│   ├── StripeService       ← Checkout Sessions + webhook processing
│   ├── WebhookService      ← Idempotent event deduplication
│   ├── AuthService         ← JWT + refresh token rotation
│   ├── ProductService      ← CRUD with cache
│   └── PlayerService       ← Lookup, search, auto-create
├── controller/
│   ├── StoreController     ← Public: products, checkout
│   ├── WebhookController   ← Raw String body for signature verification
│   ├── PluginController    ← Pending orders, fulfillment confirm
│   └── AdminController     ← Full admin dashboard API
├── exception/              ← Global error handling → consistent JSON
└── task/                   ← Webhook retry (5min), cleanup (daily)
```

### Frontend (`ecorank-frontend/`)

**Tech:** React 18, Vite, TailwindCSS, TanStack Query, Recharts, Axios

```
src/
├── api/          ← Axios-based API clients per domain
├── hooks/        ← React Query hooks (useProducts, useOrders, etc.)
├── context/      ← AuthContext with in-memory token + refresh
├── components/
│   ├── common/   ← DataTable, StatusBadge, ConfirmDialog, etc.
│   ├── layout/   ← AdminLayout (sidebar), StoreLayout
│   ├── stats/    ← RevenueChart, TopProducts, RecentOrders
│   └── domain/   ← ProductForm, RefundButton, PlayerSearch
├── pages/        ← Route pages (Dashboard, Products, Orders, etc.)
└── lib/          ← Axios instance + JWT interceptor, formatters
```

**Auth:** Access token held in memory (React ref), refresh token in HttpOnly cookie. Axios interceptor handles 401 → refresh → retry transparently.

---

## Database Schema

```
┌───────────────┐     ┌────────────────┐     ┌───────────────┐
│   players     │     │    products    │     │  admin_users  │
├───────────────┤     ├────────────────┤     ├───────────────┤
│ id (PK)       │     │ id (PK)        │     │ id (PK)       │
│ minecraft_uuid│←┐   │ slug (UNIQUE)  │     │ username (UQ) │
│ username      │ │   │ name           │     │ password_hash │
│ email         │ │   │ description    │     │ role          │
│ created_at    │ │   │ price_cents    │     │ created_at    │
│ updated_at    │ │   │ rank_group     │     └───────────────┘
└───────────────┘ │   │ active         │
                  │   │ sort_order     │     ┌────────────────┐
                  │   │ created_at     │     │ refresh_tokens │
                  │   └────────────────┘     ├────────────────┤
                  │            ↑              │ id (PK)        │
                  │            │              │ token_hash (UQ)│
              ┌───┴────────────┴──┐          │ admin_id       │
              │      orders       │          │ expires_at     │
              ├───────────────────┤          │ revoked        │
              │ id (PK)           │          │ created_at     │
              │ player_id (FK)    │          └────────────────┘
              │ product_id (FK)   │
              │ idempotency_key   │←── UUID from frontend (dedup)
              │ amount_cents      │←── Snapshot at purchase time
              │ status            │←── VARCHAR: state machine
              │ payment_provider  │
              │ provider_payment_id│
              │ server_id         │
              │ fulfilled_at      │
              │ created_at        │
              │ updated_at        │
              └───────────────────┘

              ┌───────────────────┐
              │  webhook_events   │
              ├───────────────────┤
              │ id (PK)           │
              │ event_id (UNIQUE) │←── Stripe/PayPal event dedup
              │ provider          │
              │ event_type        │
              │ payload (JSONB)   │←── Full audit trail
              │ processed         │
              │ processed_at      │
              │ created_at        │
              └───────────────────┘
```

**Key design decisions:**
- **All prices as integer cents** — `price_cents INTEGER`, never float/decimal
- **Order snapshots price** — `amount_cents` copied from product at purchase time (price changes don't affect past orders)
- **VARCHAR for status** — Cross-database compatible (works on PostgreSQL and H2)
- **Webhook dedup** — `event_id UNIQUE` prevents duplicate processing

**Indexes:**
- `orders(player_id, status)` — Plugin pending order queries
- `orders(provider_payment_id)` — Webhook lookup by Stripe payment ID
- `players(minecraft_uuid)` — Player lookup
- `products(active, sort_order)` — Store listing

---

## Payment Flow (End-to-End)

```
Player          Frontend         Backend           Stripe          Plugin
  │                │                │                │               │
  │  Browse store  │                │                │               │
  │───────────────►│ GET /store/    │                │               │
  │                │  products      │                │               │
  │                │───────────────►│                │               │
  │  ◄─────────────│ Product list   │                │               │
  │                │                │                │               │
  │  Click "Buy"   │                │                │               │
  │───────────────►│ POST /store/   │                │               │
  │  (+ username)  │  checkout      │                │               │
  │                │───────────────►│ Create Session  │               │
  │                │                │───────────────►│               │
  │                │                │ ◄──────────────│               │
  │                │ ◄──────────────│ checkoutUrl    │               │
  │  Redirect      │                │                │               │
  │═══════════════════════════════════════════════►│               │
  │                │                │                │               │
  │  Pay on Stripe │                │                │               │
  │───────────────────────────────────────────────►│               │
  │                │                │   Webhook      │               │
  │                │                │◄───────────────│               │
  │                │                │ Verify sig     │               │
  │                │                │ Dedup check    │               │
  │                │                │ Create order   │               │
  │                │                │ Status: QUEUED │               │
  │                │                │                │               │
  │                │                │         GET /plugin/pending    │
  │                │                │◄──────────────────────────────│
  │                │                │ Return orders  │               │
  │                │                │──────────────────────────────►│
  │                │                │                │     Apply rank│
  │                │                │                │   (LuckPerms)│
  │                │                │      POST /plugin/confirm     │
  │                │                │◄──────────────────────────────│
  │                │                │ Status: FULFILLED              │
```

---

## Refund Flow

Two entry points, same result:

1. **Inbound** (Stripe webhook): `charge.refunded` event → backend marks REFUNDED → queues rank removal
2. **Outbound** (admin dashboard): Admin clicks Refund → backend calls Stripe Refund API → same flow

Rank removal handling:
- **Player online:** Plugin removes LuckPerms group immediately on next poll
- **Player offline:** Stored in `pending_deliveries.json`, applied on `PlayerJoinEvent`

---

## Security Model

### Three-Tier Authentication

```
Request → SecurityFilterChain routing:

/api/v1/plugin/**   → ApiKeyFilter → checks X-API-Key header
/api/v1/admin/**    → JwtAuthFilter → validates Bearer token
/api/v1/store/**    → permitAll (no auth)
/api/v1/webhooks/** → permitAll (signature verified in service layer)
/actuator/health    → permitAll
Everything else     → deny
```

### Token Strategy

- **Admin:** JWT access token (15-min, in-memory) + refresh token (7-day, HttpOnly cookie, SHA-256 hashed in DB, rotated on use)
- **Plugin:** Shared API key (`X-API-Key` header) — simpler than JWT for machine-to-machine
- **Webhooks:** Stripe signature verification (HMAC-SHA256 on raw body)

### Rate Limiting

Bucket4j with per-IP token buckets:
- Store: 60/min
- Webhooks: 100/min
- Plugin: 120/min
- Admin: 300/min

---

## Plugin Threading Model

```
                    ┌──────────────────────┐
                    │  FulfillmentPollTask  │
                    │  (async thread)      │
                    └──────────┬───────────┘
                               │
                    1. HTTP GET pending orders
                               │
                    ┌──────────▼───────────┐
                    │   Main Server Thread  │
                    │   (runTask)          │
                    └──────────┬───────────┘
                               │
                    2. Apply rank (LuckPerms)
                    3. Send player message
                    4. Deliver items
                               │
                    ┌──────────▼───────────┐
                    │   Async Thread       │
                    │   (runTaskAsync)     │
                    └──────────────────────┘
                               │
                    5. HTTP POST confirm fulfillment
```

**Why polling, not WebSocket?** Self-healing on backend restart, no reconnection logic, 5s latency is acceptable for rank delivery. Exponential backoff (5s → 30s) reduces idle load.

---

## Deployment Architecture

```
docker-compose.yml:

┌──────────────────────────────────────────┐
│  backend (port 8080)                     │
│  ┌────────────────────────────────────┐  │
│  │  Spring Boot JAR                   │  │
│  │  + React SPA (static files)        │  │
│  └────────────────────────────────────┘  │
│      │                    │              │
│      ▼                    ▼              │
│  ┌────────┐        ┌──────────┐         │
│  │Postgres│        │  Redis   │         │
│  │  :5432 │        │  :6379   │         │
│  └────────┘        └──────────┘         │
└──────────────────────────────────────────┘

External:
  ┌─────────────────┐     ┌──────────────┐
  │ Paper Server(s) │     │    Stripe    │
  │ + EcoRank.jar   │────►│   Webhooks   │───►:8080
  └─────────────────┘     └──────────────┘
```

**Multi-stage Dockerfile:** Node builds frontend → Maven builds backend with frontend in `resources/static/` → JRE runtime image. Single container serves both API and SPA.
