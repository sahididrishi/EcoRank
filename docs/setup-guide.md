# Setup Guide

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- Docker & Docker Compose
- Stripe account (test mode for development)
- Paper 1.21.x Minecraft server with LuckPerms

## Quick Start (Docker)

1. Clone the repository
2. Copy `.env.example` to `.env` and fill in values
3. Run `docker-compose up`
4. Access the admin dashboard at `http://localhost:8080`

## Development Setup

### Backend
```bash
cd ecorank-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
The dev profile uses H2 in-memory database and embedded Redis.

### Frontend
```bash
cd ecorank-frontend
npm install
npm run dev
```
Dev server runs at `http://localhost:5173` with API proxy to `localhost:8080`.

### Plugin
```bash
cd ecorank-plugin
mvn clean package
```
Copy the JAR from `target/` to your Paper server's `plugins/` directory.

## Environment Variables

See `.env.example` for all required variables.

## Stripe Setup

1. Create a Stripe account at https://stripe.com
2. Get your test API keys from the Stripe Dashboard
3. Set up a webhook endpoint pointing to `https://your-domain/api/v1/webhooks/stripe`
4. Subscribe to events: `checkout.session.completed`, `charge.refunded`
5. Copy the webhook signing secret to `STRIPE_WEBHOOK_SECRET`

## Plugin Installation

1. Build the plugin JAR: `cd ecorank-plugin && mvn clean package`
2. Copy `target/ecorank-plugin-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` folder
3. Install LuckPerms on the server
4. Start the server — EcoRank will generate `plugins/EcoRank/config.yml`
5. Edit `config.yml` to set `backend.url` and `backend.api-key`
6. Restart the server
