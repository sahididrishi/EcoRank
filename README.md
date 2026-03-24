# EcoRank

Self-hosted Minecraft server economy + web donation store + rank management. Zero third-party transaction fees alternative to Tebex/Buycraft.

## Modules

- **ecorank-plugin/** — Paper 1.21.x plugin (Java 21, Maven)
- **ecorank-backend/** — Spring Boot 3.2 REST API (PostgreSQL, Redis)
- **ecorank-frontend/** — React 18 admin dashboard (Vite, TailwindCSS)

## Quick Start

```bash
# Copy and configure environment variables
cp .env.example .env

# Launch full stack
docker-compose up

# Access
# Admin dashboard: http://localhost:8080
# API docs (dev): http://localhost:8080/swagger-ui.html
# Health check:   http://localhost:8080/actuator/health
```

## Development

### Backend
```bash
cd ecorank-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend
```bash
cd ecorank-frontend
npm install && npm run dev
```

### Plugin
```bash
cd ecorank-plugin
mvn clean package
# Copy target/ecorank-plugin-*.jar to Paper server plugins/
```

## Documentation

- [Developer Machine Setup](docs/developer-setup.md) — Full guide for setting up on a new PC
- [Setup Guide](docs/setup-guide.md) — Production deployment, Stripe, plugin install
- [API Specification](docs/api-spec.md) — All endpoints with request/response examples
- [Architecture](docs/architecture.md) — System design, DB schema, payment flows
