# Developer Machine Setup Guide

Complete guide for setting up the EcoRank development environment on a fresh PC.

---

## Step 1: Install Prerequisites

### Java 21 (Required)

**Windows:**
```powershell
# Option A: Microsoft Build of OpenJDK (recommended)
winget install Microsoft.OpenJDK.21

# Option B: Manual download
# https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21
```

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Verify:**
```bash
java -version
# Should show: openjdk version "21.x.x"
```

---

### Maven 3.9+ (Required)

**Windows:**
```powershell
winget install Apache.Maven
```

**macOS:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt install maven
```

**Manual install (any OS):**
```bash
# Download from https://maven.apache.org/download.cgi
# Extract to a directory, add bin/ to PATH
```

**Verify:**
```bash
mvn --version
# Should show: Apache Maven 3.9.x and Java 21
```

---

### Node.js 18+ (Required)

**Windows:**
```powershell
winget install OpenJS.NodeJS.LTS
```

**macOS:**
```bash
brew install node@18
```

**Linux:**
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs
```

**Verify:**
```bash
node --version   # Should show v18.x.x or higher
npm --version    # Should show 9.x.x or higher
```

---

### Docker & Docker Compose (Required for full stack)

**Windows:**
```powershell
winget install Docker.DockerDesktop
# Restart PC after install. Open Docker Desktop and let it start.
```

**macOS:**
```bash
brew install --cask docker
# Open Docker.app from Applications
```

**Linux:**
```bash
sudo apt install docker.io docker-compose-v2
sudo usermod -aG docker $USER
# Log out and back in for group change to take effect
```

**Verify:**
```bash
docker --version          # Should show Docker version 24+
docker compose version    # Should show v2.x.x
```

---

### Git (Required)

**Windows:**
```powershell
winget install Git.Git
```

**macOS/Linux:** Usually pre-installed. If not:
```bash
# macOS
brew install git

# Linux
sudo apt install git
```

---

## Step 2: Transfer the Codebase

### Option A: From USB / File Transfer

```bash
# Copy the EcoRank folder to your new machine, then:
cd EcoRank
git status    # Should show clean working tree
```

### Option B: From GitHub (if you pushed)

```bash
git clone https://github.com/YOUR_USERNAME/EcoRank.git
cd EcoRank
```

### Option C: From ZIP

```bash
# On the OLD machine, create a zip with git history:
cd /path/to/EcoRank
git bundle create EcoRank.bundle --all
# Transfer EcoRank.bundle to new machine

# On the NEW machine:
git clone EcoRank.bundle EcoRank
cd EcoRank
```

> **Tip:** `git bundle` preserves full commit history, unlike a zip of the folder.

---

## Step 3: Configure Git Identity

```bash
cd EcoRank
git config user.name "Your Name"
git config user.email "your@email.com"
```

---

## Step 4: Set Up Each Module

### 4a. Backend (Spring Boot)

```bash
cd ecorank-backend

# Download all Maven dependencies (first time takes a few minutes)
mvn dependency:resolve

# Compile to verify everything works
mvn clean compile

# Run in dev mode (H2 in-memory DB, no Docker needed)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Verify:** Open `http://localhost:8080/actuator/health` — should return `{"status":"UP"}`

**Swagger docs:** Open `http://localhost:8080/swagger-ui.html` (dev profile only)

Default admin login: `admin` / `admin`

---

### 4b. Frontend (React)

```bash
cd ecorank-frontend

# Install npm dependencies
npm install

# Start dev server
npm run dev
```

**Verify:** Open `http://localhost:5173` — should show the store page.

**Note:** The frontend dev server proxies `/api` requests to `localhost:8080`, so start the backend first.

---

### 4c. Plugin (Paper)

```bash
cd ecorank-plugin

# Download dependencies and build the JAR
mvn clean package
```

**Verify:** JAR file appears at `target/ecorank-plugin-1.0.0-SNAPSHOT.jar` (~3.7 MB)

**To test on a server:**
1. Download Paper 1.21.4 from https://papermc.io/downloads/paper
2. Download LuckPerms from https://luckperms.net/download
3. Place both JARs in the server's `plugins/` folder
4. Start the server: `java -jar paper-1.21.4.jar`
5. Edit `plugins/EcoRank/config.yml` with backend URL
6. Restart

---

## Step 5: Full Stack with Docker (Production-like)

```bash
cd EcoRank

# Create your environment file
cp .env.example .env
```

**Edit `.env` with real values:**
```env
POSTGRES_PASSWORD=pick-a-strong-password
REDIS_PASSWORD=pick-another-password
JWT_SIGNING_KEY=run-openssl-rand-hex-32-to-generate
PLUGIN_API_KEY=run-openssl-rand-hex-32-to-generate
STRIPE_SECRET_KEY=sk_test_your_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_secret_here
```

**Generate random keys:**
```bash
# Linux/macOS
openssl rand -hex 32

# Windows (PowerShell)
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })

# Or just use any password generator for 64-character hex strings
```

**Launch:**
```bash
docker compose up --build
```

First build takes 5-10 minutes (downloads dependencies). Subsequent builds are cached.

**Verify:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Open admin dashboard
# http://localhost:8080/login  (admin/admin)

# Open store
# http://localhost:8080/store
```

**Stop:**
```bash
docker compose down          # Stop containers (keep data)
docker compose down -v       # Stop and delete database data
```

---

## Step 6: Run Tests

### Plugin Tests (no Docker needed)

```bash
cd ecorank-plugin
mvn test
# 56 tests should pass
```

### Backend Tests (needs Docker running)

```bash
cd ecorank-backend
mvn test
# Testcontainers will spin up PostgreSQL + Redis automatically
# Docker must be running for this to work
```

### Frontend Lint + Type Check

```bash
cd ecorank-frontend
npm run lint      # ESLint check
npm run build     # TypeScript type check + production build
```

---

## IDE Setup (Optional but Recommended)

### IntelliJ IDEA

1. Open → Select the `EcoRank` root folder
2. IntelliJ will detect the Maven modules automatically
3. Set Project SDK to Java 21: File → Project Structure → Project → SDK
4. Import Maven projects when prompted
5. For frontend: Install the "Prettier" and "Tailwind CSS" plugins

### VS Code

1. Open the `EcoRank` folder
2. Install recommended extensions:
   - **Java Extension Pack** (Microsoft)
   - **Spring Boot Extension Pack** (VMware)
   - **ES7+ React/Redux/React-Native snippets**
   - **Tailwind CSS IntelliSense**
   - **Prettier**
   - **ESLint**
3. For Java, VS Code will prompt to configure JDK — point to Java 21

---

## Common Issues on New Machine

### "mvn: command not found"
Maven isn't in PATH. Add Maven's `bin/` directory to your system PATH.

### "JAVA_HOME is not set"
```bash
# Find where Java is installed, then:
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.x.x"

# Linux/macOS (add to ~/.bashrc or ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### "Class file has wrong version 65.0, should be 61.0"
You're compiling with Java 17 but Paper 1.21.x needs Java 21. Install Java 21.

### "npm: command not found"
Node.js isn't installed or not in PATH.

### "Docker daemon not running"
Start Docker Desktop (Windows/macOS) or `sudo systemctl start docker` (Linux).

### "Port 8080 already in use"
Another app is using port 8080. Either stop it or change the port:
```bash
# Backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dserver.port=9090

# Docker
# Edit docker-compose.yml: change "8080:8080" to "9090:8080"
```

### Maven downloads take forever
First build downloads ~500MB of dependencies. This is normal. Subsequent builds use the local cache (`~/.m2/repository/`).

**To transfer Maven cache between machines** (saves 10+ minutes):
```bash
# On old machine: zip ~/.m2/repository
# On new machine: extract to ~/.m2/repository
```

---

## Quick Reference: Daily Development Commands

```bash
# Start backend (dev mode)
cd ecorank-backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Start frontend (dev mode, in separate terminal)
cd ecorank-frontend && npm run dev

# Build plugin JAR
cd ecorank-plugin && mvn clean package

# Run all plugin tests
cd ecorank-plugin && mvn test

# Run backend tests (Docker must be running)
cd ecorank-backend && mvn test

# Full stack Docker
docker compose up --build

# Lint frontend
cd ecorank-frontend && npm run lint

# Production build frontend
cd ecorank-frontend && npm run build
```
