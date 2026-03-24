# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Summary

EcoRank is a full-stack Minecraft server plugin combining an in-game economy system with a web-based donation store and rank management panel. Self-hosted, zero third-party transaction fees alternative to Tebex/Buycraft. Server owners host the backend themselves with full control over store UI, payment flows, and player data.

## Repository Structure

Mono-repo with three modules:

- **ecorank-plugin/** — Paper 1.21.x plugin JAR (Maven, Java 17)
- **ecorank-backend/** — Spring Boot 3.2 REST API
- **ecorank-frontend/** — React 18 admin dashboard (Vite, TailwindCSS)
- **docker-compose.yml** — Full stack launcher (Spring Boot + PostgreSQL + Redis)
- **docs/** — Setup guide + API spec

## Build Commands

### Plugin (Maven)
```bash
cd ecorank-plugin
mvn clean package           # Build plugin JAR
mvn test                    # Run JUnit 5 tests
mvn test -Dtest=ClassName   # Run a single test class
```

### Backend (Spring Boot / Maven)
```bash
cd ecorank-backend
mvn spring-boot:run         # Run the backend locally
mvn clean package           # Build JAR
mvn test                    # Run all tests (uses Testcontainers for PostgreSQL + Redis)
mvn test -Dtest=ClassName   # Run a single test class
```

### Frontend (Vite)
```bash
cd ecorank-frontend
npm install
npm run dev                 # Dev server
npm run build               # Production build
npm run lint                # Lint
```

### Full Stack (Docker)
```bash
docker-compose up           # Launch everything: Spring Boot + PostgreSQL 16 + Redis 7
```

## Architecture

### Two Decoupled Systems

The plugin and backend communicate over a **secured internal HTTP API** — they do not share a database directly.

**Payment & rank grant flow:**
1. Player visits web storefront, checks out via Stripe or PayPal
2. Backend receives payment webhook, verifies signature (HMAC-SHA256), creates order
3. Order placed in Redis fulfillment queue (Redis List via `RPUSH`, not Pub/Sub — messages persist if plugin is offline)
4. Plugin polls backend every 5s for pending orders (with exponential backoff to 30s when idle)
5. Plugin applies rank via LuckPerms API on the **main thread** (or queues item delivery for offline players)
6. Plugin confirms fulfillment back to backend asynchronously

### Plugin Layer

**Internal structure follows a service-layer pattern** — avoid the Bukkit anti-pattern of a god-class main plugin. The `EcoRankPlugin.java` entry point is thin (wiring only), with separate packages for `command/`, `listener/`, `service/`, `storage/`, `task/`, `api/`, and `model/`.

- Economy core: per-player balance in SQLite (default) or MySQL (networked), via `StorageProvider` interface with HikariCP connection pooling
- Event listeners award configurable coins: mob kills, daily login, chest quests, player trading
- LuckPerms integration for rank grants and removals (including refund-triggered removal)
- PlaceholderAPI support (`%ecorank_balance%`, `%ecorank_rank%`)
- All tuneable values in `config.yml` via typed `ConfigService` wrapper — fail fast on missing required values at startup
- Commands registered via Paper's `LifecycleEventManager` with Brigadier-style tab completion

**Critical async threading rule:** HTTP calls to backend must run asynchronously (`runTaskAsynchronously`), but all Bukkit/LuckPerms API calls (player, world, inventory, permission modifications) must run on the **main server thread** (`runTask`). Getting this wrong causes `IllegalStateException: Asynchronous entity/player access`.

### Backend Layer

**Three API tiers with different auth:**
- **Public** (`/api/v1/store/**`): No auth — storefront product listing and checkout session creation
- **Plugin-internal** (`/api/v1/plugin/**`): API key in `X-API-Key` header (not JWT — simpler and equivalent security for machine-to-machine)
- **Admin** (`/api/v1/admin/**`): JWT access token (15-min) + refresh token rotation (7-day, HttpOnly cookie)
- **Webhooks** (`/api/v1/webhooks/**`): Signature-verified, no JWT

Key components:
- Spring Boot 3.2 REST API with Spring Security (`SecurityFilterChain` partitions the three auth strategies)
- PostgreSQL 16 for orders, products, players — **Flyway** for migrations (`src/main/resources/db/migration/`)
- Redis 7 for fulfillment queue (List), caching (product catalog 5-min TTL, player profiles 10-min TTL), rate limiting (Bucket4j)
- Stripe SDK (Checkout Sessions + webhooks) and PayPal REST API (webhook verification)
- Prometheus metrics via Spring Actuator (`/actuator/metrics`)
- OpenAPI 3.0 / Swagger auto-generated docs
- MapStruct for entity-to-DTO conversion (never expose JPA entities in API responses)

**Webhook idempotency:** `webhook_events` table deduplicates by `event_id`. Before processing any webhook: insert with `ON CONFLICT DO NOTHING` — if 0 rows affected, return 200 immediately. A scheduled job retries unprocessed events older than 5 minutes.

### Frontend Layer
- React 18 + Vite + TailwindCSS
- Recharts for revenue charts, React Query (TanStack Query) for server-state management — no Redux/Zustand needed
- Admin dashboard: product management, order history, revenue stats, refund actions, player lookup
- Build artifact served by Spring Boot — no separate hosting needed
- Auth: access token in memory (React ref, not localStorage), refresh via HttpOnly cookie, Axios interceptor handles 401 → refresh → retry

## Critical Design Decisions

### Money Storage
**Always store prices as integer cents** (e.g., `$19.99` → `1999`). Never use DECIMAL or float. The `orders` table snapshots `amount_cents` at purchase time — do not reference the product's current price via a join (price changes must not retroactively alter historical orders).

### Idempotency Keys
Frontend generates a UUID v4 when user clicks "Buy Now", sent with checkout request, passed through to Stripe's `idempotencyKey`. Backend checks `idempotency_key` column on `orders` before creating. Prevents duplicate orders from double-clicks or retries.

### Refund Handling (bidirectional)
- **Inbound** (Stripe webhook): `charge.refunded` → mark order `REFUNDED` → queue rank removal in Redis → plugin removes LuckPerms group
- **Outbound** (admin dashboard): Admin clicks refund → backend calls Stripe Refund API → same flow
- **Offline player edge case**: Maintain `pending_rank_removals` list. On `PlayerJoinEvent`, check and remove rank.

### Plugin Polling vs WebSocket/SSE
Polling with exponential backoff is the correct choice here. WebSocket/SSE add reconnection complexity with no user-visible benefit (5s vs sub-second latency doesn't matter for rank delivery). Polling is self-healing if the backend restarts.

### Plugin-to-Backend Auth
Use shared API key (`X-API-Key` header), not JWT. Plugin also sends `X-Server-Id` header for multi-server network tracking.

## Database Schema Notes

Key tables: `players`, `products`, `orders`, `webhook_events`. Important columns:
- `orders.idempotency_key` (UUID, UNIQUE) — deduplication
- `orders.amount_cents` (INTEGER) — price snapshot
- `orders.status` enum: `PENDING_PAYMENT → PAID → QUEUED → FULFILLED → REFUNDED → FAILED`
- `webhook_events.event_id` (UNIQUE) — Stripe/PayPal event dedup
- `webhook_events.payload` (JSONB) — full audit trail
- `products.price_cents` (INTEGER) — never float/decimal
- `products.slug` (UNIQUE) — URL-friendly identifier

Key indexes: `orders(player_id, status)`, `orders(provider_payment_id)`, `players(minecraft_uuid)`, `products(active, sort_order)`.

## Stripe Webhook Integration

**Critical:** Accept the raw request body as `@RequestBody String payload`, not a DTO. If Jackson parses and re-serializes the body, the signature will never match. Use `Webhook.constructEvent(payload, sigHeader, webhookSecret)` from the Stripe SDK.

## Environment & Secrets

Secrets are environment variables or Docker secrets, never committed:
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `DATABASE_URL`, `REDIS_PASSWORD`, `JWT_SIGNING_KEY`, `PLUGIN_API_KEY`

Spring profiles: `dev` (H2/embedded, verbose logging, CORS `*`, Swagger enabled), `prod` (PostgreSQL, strict CORS, Swagger disabled, Actuator restricted to `/health` + `/metrics`).

## Testing Strategy

### Backend Integration Tests (highest value)
Use Testcontainers with a shared abstract `IntegrationTestBase` class (avoids spinning up new containers per test class):
1. **Webhook end-to-end**: POST realistic Stripe payload → assert order in PostgreSQL with status `QUEUED` + entry in Redis fulfillment queue
2. **Duplicate webhook idempotency**: POST same webhook twice → assert no duplicate order
3. **Plugin polling + confirmation**: GET pending orders → POST confirm fulfillment → assert status `FULFILLED`
4. **Refund flow**: Create paid order → POST refund webhook → assert `REFUNDED` + rank removal queued

### Plugin Tests
Mock `BackendClient` and LuckPerms API with Mockito. Test `EconomyService` with in-memory SQLite. Test config validation (missing `backend-url`).

### Frontend Tests
React Testing Library + MSW (Mock Service Worker) for mocking backend responses.

## Graceful Shutdown

- **Backend**: `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s` — ensures in-flight webhook processing completes
- **Plugin**: `onDisable()` cancels scheduled tasks, flushes pending fulfillment confirmations synchronously, closes HikariCP connections
