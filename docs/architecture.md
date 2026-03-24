# Architecture

## System Overview

EcoRank consists of three modules communicating over HTTP:

```
┌─────────────┐     HTTP/REST      ┌─────────────────┐
│  Minecraft   │◄──────────────────►│  Spring Boot    │
│  Plugin      │   X-API-Key auth   │  Backend        │
│  (Paper)     │                    │                 │
└─────────────┘                    │  ┌───────────┐  │
                                   │  │PostgreSQL │  │
┌─────────────┐                    │  └───────────┘  │
│  React SPA   │◄──────────────────►│  ┌───────────┐  │
│  Frontend    │   JWT auth         │  │  Redis    │  │
└─────────────┘                    │  └───────────┘  │
                                   └─────────────────┘
         ▲                                  ▲
         │                                  │
    Players browse                   Stripe/PayPal
    store & checkout                 webhooks
```

## Payment Flow

1. Player visits web storefront, selects a product
2. Frontend creates checkout session via backend API
3. Backend creates Stripe Checkout Session, returns URL
4. Player completes payment on Stripe
5. Stripe sends `checkout.session.completed` webhook
6. Backend verifies signature, creates order with status QUEUED
7. Plugin polls `/plugin/orders/pending` every 5s (with backoff)
8. Plugin applies rank via LuckPerms on main thread
9. Plugin confirms fulfillment back to backend
10. Order status transitions to FULFILLED

## Refund Flow

1. Admin clicks Refund in dashboard (or Stripe sends `charge.refunded` webhook)
2. Backend marks order as REFUNDED
3. Backend queues rank removal
4. Plugin picks up removal on next poll
5. If player is online: remove rank immediately
6. If player is offline: remove rank on next join

## Plugin Polling Strategy

Exponential backoff: 5s → 10s → 15s → 30s when idle.
Resets to 5s when orders are found. Self-healing if backend restarts.
