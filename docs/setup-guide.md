# Setup Guide

## Prerequisites

- **Java 21+** (Paper 1.21.x requires Java 21)
- **Maven 3.9+**
- **Node.js 18+**
- **Docker & Docker Compose** (for production deployment)
- **Stripe account** (test mode for development)
- **Paper 1.21.x Minecraft server** with LuckPerms installed

---

## Quick Start (Docker — Production)

```bash
# 1. Clone the repository
git clone https://github.com/your-username/EcoRank.git
cd EcoRank

# 2. Configure environment
cp .env.example .env
# Edit .env with your actual values (see Environment Variables below)

# 3. Launch everything
docker-compose up -d

# 4. Verify
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

The admin dashboard is at `http://localhost:8080/login`.
Default credentials: **admin / admin** (change immediately after first login).

---

## Development Setup

### Backend (Spring Boot)

```bash
cd ecorank-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev profile:
- Uses **H2 in-memory database** (no PostgreSQL needed)
- Disables Flyway (uses Hibernate auto-DDL instead)
- Enables Swagger UI at `http://localhost:8080/swagger-ui.html`
- Allows all CORS origins
- Uses hardcoded dev secrets (see `application-dev.yml`)

### Frontend (React + Vite)

```bash
cd ecorank-frontend
npm install
npm run dev
```

Dev server at `http://localhost:5173` with API requests proxied to `localhost:8080`.

### Plugin (Paper)

```bash
cd ecorank-plugin
mvn clean package
```

Copy `target/ecorank-plugin-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` directory.

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | PostgreSQL password | `s3cureP@ss` |
| `POSTGRES_USER` | PostgreSQL user (default: ecorank) | `ecorank` |
| `POSTGRES_DB` | PostgreSQL database (default: ecorank) | `ecorank` |
| `DATABASE_URL` | Full JDBC URL | `jdbc:postgresql://postgres:5432/ecorank` |
| `REDIS_PASSWORD` | Redis password | `r3disP@ss` |
| `REDIS_HOST` | Redis host (default: localhost) | `redis` |
| `REDIS_PORT` | Redis port (default: 6379) | `6379` |
| `JWT_SIGNING_KEY` | HMAC-SHA256 key (min 32 chars) | `your-32-char-minimum-secret-key-here` |
| `PLUGIN_API_KEY` | Shared key for plugin-backend auth | `generate-with-openssl-rand-hex-32` |
| `STRIPE_SECRET_KEY` | Stripe API secret key | `sk_test_...` or `sk_live_...` |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | `whsec_...` |
| `PAYPAL_CLIENT_ID` | PayPal client ID (optional) | |
| `PAYPAL_CLIENT_SECRET` | PayPal client secret (optional) | |

Generate secure random keys:
```bash
# For JWT_SIGNING_KEY and PLUGIN_API_KEY
openssl rand -hex 32
```

---

## Stripe Setup

1. Create a Stripe account at [stripe.com](https://stripe.com)
2. Get your **test API keys** from Dashboard → Developers → API keys
3. Set `STRIPE_SECRET_KEY` in `.env`

### Webhook Configuration

4. Go to Dashboard → Developers → Webhooks → Add endpoint
5. Set URL to `https://your-domain/api/v1/webhooks/stripe`
6. Select events:
   - `checkout.session.completed`
   - `charge.refunded`
7. Copy the **Signing secret** to `STRIPE_WEBHOOK_SECRET` in `.env`

### Local Testing with Stripe CLI

```bash
# Install Stripe CLI, then:
stripe login
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# In another terminal, trigger test events:
stripe trigger checkout.session.completed
stripe trigger charge.refunded
```

---

## Plugin Installation

1. **Build** the plugin: `cd ecorank-plugin && mvn clean package`
2. **Copy** `target/ecorank-plugin-1.0.0-SNAPSHOT.jar` to `plugins/`
3. **Install LuckPerms** (required dependency)
4. **Start** the server — EcoRank generates `plugins/EcoRank/config.yml`
5. **Configure** `config.yml`:

```yaml
backend:
  url: http://your-backend:8080    # Backend URL
  api-key: your-plugin-api-key     # Must match PLUGIN_API_KEY in .env
  server-id: survival-1            # Unique ID for this server
```

6. **Restart** the server

### Plugin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/balance [player]` | Check balance | `ecorank.balance` (default: true) |
| `/pay <player> <amount>` | Send coins to another player | `ecorank.pay` (default: true) |
| `/baltop [page]` | View top balances leaderboard | `ecorank.baltop` (default: true) |
| `/eco give <player> <amount>` | Give coins (admin) | `ecorank.admin` |
| `/eco take <player> <amount>` | Take coins (admin) | `ecorank.admin` |
| `/eco set <player> <amount>` | Set balance (admin) | `ecorank.admin` |

### Plugin Placeholders (PlaceholderAPI)

| Placeholder | Output |
|-------------|--------|
| `%ecorank_balance%` | Formatted balance (e.g., `$1,250`) |
| `%ecorank_balance_raw%` | Raw balance number (e.g., `1250`) |
| `%ecorank_rank%` | Player's primary LuckPerms group |
| `%ecorank_currency%` | Currency name from config |

### Plugin Config Reference

Key `config.yml` sections:

```yaml
storage:
  type: sqlite              # sqlite or mysql

economy:
  starting-balance: 100     # New player starting balance
  currency-name: Coins
  currency-symbol: '$'

rewards:
  mob-kill:
    zombie: 5
    skeleton: 7
    creeper: 10
    ender_dragon: 500
  daily-login: 50
  chest-quest:
    enabled: true
    reward: 25
    cooldown-minutes: 30
```

---

## Multi-Server Setup

For server networks (BungeeCord/Velocity):

1. Each Paper server runs its own EcoRank plugin instance
2. All instances connect to the **same backend** URL
3. Set a unique `server-id` in each server's `config.yml`
4. Use **MySQL** storage type (shared across servers) instead of SQLite
5. The backend tracks which server fulfilled each order via `X-Server-Id`

---

## Troubleshooting

### Backend won't start
- Check `DATABASE_URL` is correct and PostgreSQL is running
- Ensure `JWT_SIGNING_KEY` is at least 32 characters
- Check logs: `docker-compose logs backend`

### Plugin says "Failed to connect to backend"
- Verify `backend.url` in `config.yml` is reachable from the Minecraft server
- Verify `backend.api-key` matches `PLUGIN_API_KEY` in the backend's `.env`
- Check the backend is running: `curl http://backend-host:8080/actuator/health`

### Stripe webhooks not working
- Verify `STRIPE_WEBHOOK_SECRET` matches the webhook endpoint's signing secret
- Check webhook delivery in Stripe Dashboard → Developers → Webhooks → Select endpoint → Recent deliveries
- For local testing, use `stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe`

### Ranks not being applied
- Verify LuckPerms is installed and the group exists: `/lp group <name> info`
- Check the plugin is polling: look for "Fetched X pending orders" in server logs
- Verify the order status is QUEUED in the admin dashboard
