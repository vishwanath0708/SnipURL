# 🚀 SnipURL - Enterprise URL Shortener Service

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Coverage Status](https://img.shields.io/badge/coverage-84%25-green.svg)](#)
[![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue.svg)](#)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](#)
[![Docker Pulls](https://img.shields.io/badge/docker%20pulls-latest-orange.svg)](https://hub.docker.com/r/vishwahubballi/snipurl)

SnipURL is a high-performance, enterprise-grade URL shortening service that transforms long, cumbersome URLs into clean, manageable links. Architected to support sub-millisecond redirect lookups, the service integrates secure authentication, distributed caching, intelligent rate-limiting, and detailed geolocated click analytics.

Whether you need to shorten links for SMS marketing campaigns, track click attributions, or manage dynamic URL lifetimes, SnipURL provides a robust web interface and a fully-featured REST API to meet production demands.

---

## 📖 Overview

In modern web applications, sharing raw URLs with query parameters can lead to broken links, poor user experiences, and lack of insights. **SnipURL** solves this by providing a unified gateway to shorten, redirect, and monitor links.

Designed with a high-throughput, read-heavy architecture, SnipURL utilizes **Spring Boot 3.2.4** for routing and business logic, **PostgreSQL 15** for persistence, and **Redis 7** for dynamic caching. Every time a shortened link is accessed:
1. **Dynamic Caching**: Redis resolves the target URL directly from memory in sub-milliseconds.
2. **Asynchronous Click Analytics**: An asynchronous job increments click counts and resolves client IP locations to countries using the **MaxMind GeoIP2** database.
3. **Robust Rate-Limiting**: To prevent denial of service and scraping, endpoints are guarded by token bucket rate-limiters powered by **Bucket4j** stored in Redis.

---

## 📐 Architecture Diagram

Below is the conceptual architecture showing request routing, cache lookups, database fallbacks, and background click tracking:

```
                          +------------------+
                          |   HTTP Clients   |
                          +--------+---------+
                                   |
                                   | HTTP Requests (API / Short Codes)
                                   v
                      +------------+------------+
                      |    Spring Boot App      |
                      +------------+------------+
                                   |
           +-----------------------+-----------------------+
           |                       |                       |
           v (Auth Checks)         v (Cache & Limit)       v (Async Tasks)
   +-------+-------+       +-------+-------+       +-------+-------+
   |    Spring     |       |    Redis 7    |       | MaxMind GeoIP |
   |   Security    |       |  (Cache/Rate  |       |   Database    |
   | (JWT Filters) |       |   Limiting)   |       | (IP Lookup)   |
   +---------------+       +-------+-------+       +---------------+
                                   |
                                   | (Cache Miss / Sync)
                                   v
                           +-------+-------+
                           | PostgreSQL 15 |
                           | (Data Store)  |
                           +---------------+
```

### Request Flow
1. **URL Reduction (`POST /api/shorten`)**:
   - Validation & Authentication (JWT validation via Spring Security).
   - Generates a unique short code or accepts custom input.
   - Persists to PostgreSQL.
   - Warms up the Redis cache with a configurable Time-To-Live (TTL).

2. **Redirection (`GET /{shortCode}`)**:
   - Dynamic lookup checks the **Redis Cache** first.
   - **Cache Hit**: Immediate `302 Found` redirection using the cached long URL.
   - **Cache Miss**: Fallback to database query. On match, updates Redis cache and completes redirection.
   - **Click Tracker**: Spawns an asynchronous task to resolve the client IP location, store analytics data, and update click counters.

---

## ✨ Features

- 🔗 **Robust URL Shortening**: Convert long URLs with support for custom backhalves and expiration timestamps.
- ⚡ **Sub-Millisecond Resolution**: High-performance redirects powered by Redis caching.
- 🔒 **Secure Authentication**: End-to-end user registration and JWT-based session security.
- 📊 **Detailed Click Analytics**: Tracks total clicks, unique visitors, daily trends, top countries (via GeoIP), and device distribution.
- ⏳ **Custom Lifetimes**: Set URLs to automatically expire and render invalid after a specific duration or date.
- 🛡️ **IP Rate Limiting**: Built-in rate limiting using Bucket4j to prevent API abuse.
- 🐳 **Docker-First Deployment**: Native Docker, multi-container Docker Compose, and Jenkins-ready CI/CD configurations.

---

## 🛠️ Tech Stack Table

| Technology | Version | Purpose |
| :--- | :--- | :--- |
| **Spring Boot** | 3.2.4 | Backend application framework |
| **Java (JDK)** | 17 | Core programming language |
| **PostgreSQL** | 15 | Primary relational database |
| **Redis** | 7.x (Alpine) | In-memory cache & rate limiter storage |
| **Spring Security** | 6.x (Starter) | Authentication, Authorization, and JWT filters |
| **Bucket4j** | 8.7.0 | Token-bucket rate limiting mechanism |
| **MaxMind GeoIP2** | 4.2.0 | Client IP-to-Country location resolver |
| **Thymeleaf** | 3.x | Template engine rendering the dashboard and login pages |
| **Lombok** | 1.18.x | Boilerplate code reduction (Getters, Setters, Builders) |
| **Maven** | 3.9+ | Build and dependency management tool |
| **Docker** | Latest | Containerization and runtime packaging |
| **Jenkins** | Latest | Automated CI/CD pipelines |

---

## 📋 Prerequisites

Ensure you have the following system utilities installed before building or running the project:

```bash
# Required installations
- Java Development Kit (JDK) 17
- Apache Maven 3.9+
- Docker Engine & Docker Compose
- Git
```

### Verify Installations
```bash
java -version
mvn -version
docker --version
docker-compose --version
```

---

## 🚀 Quick Start (5-Minute Run)

Get SnipURL up and running locally in minutes:

### 1. Clone the Repository
```bash
git clone https://github.com/vishwanath0708/SnipURL.git
cd SnipURL
```

### 2. Launch Supporting Services
Use Docker Compose to spin up Postgres and Redis containers in the background:
```bash
docker-compose up -d
```

### 3. Build the Application
Compile the codebase and package the executable JAR:
```bash
./mvnw clean package -DskipTests
```

### 4. Run the Application
Start the Spring Boot server:
```bash
java -jar target/SnipURL-0.0.1-SNAPSHOT.jar
```
The application will boot on [http://localhost:8080](http://localhost:8080).

---

## ⚙️ Installation & Setup

### 📦 Setup Option A: Run Pre-built Containers
If you do not want to compile code locally, pull and run pre-built Docker Hub images directly:

```bash
# 1. Start PostgreSQL
docker run -d --name snipurl-db \
  -e POSTGRES_DB=snipurl \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:15

# 2. Start Redis
docker run -d --name snipurl-cache \
  -p 6379:6379 \
  redis:7-alpine

# 3. Start the application container
docker run -d --name snipurl-app \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/snipurl \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 \
  vishwahubballi/snipurl:latest
```

---

### 💻 Setup Option B: Running via Docker Compose (Recommended)
Our preconfigured `docker-compose.yml` mounts local data volumes, establishes a secure bridge network, and sets up dependencies cleanly.

Run the system with one command:
```bash
docker-compose up -d --build
```

To view live application logs:
```bash
docker-compose logs -f app
```

To tear down services and preserve databases:
```bash
docker-compose down
```

---

## 🔧 Configuration Reference

You can customize the runtime environment by setting the following environment variables. If left unset, default configurations from `application.properties` are used:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/snipurl` | PostgreSQL JDBC connection endpoint |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `secret` | PostgreSQL password |
| `SHOW_SQL` | `true` | Toggle SQL command logs in the application console |
| `APP_BASE_URL` | `http://localhost:8080` | Root URL prepended to generated short codes |
| `JWT_SECRET` | `404E635266556...` | HS256 JWT HMAC key (Minimum 256 bits for production!) |
| `JWT_EXPIRATION` | `86400000` | Token lifespan in milliseconds (Default: 24 hours) |
| `REDIS_HOST` | `localhost` | Redis server address |
| `REDIS_PORT` | `6379` | Redis listener port |
| `REDIS_PASSWORD` | *(empty)* | Optional password to secure the Redis connection |
| `CACHE_TTL` | `3600` | Caching dynamic URL mappings duration in seconds (1 hour) |

---

## 📖 Usage Guide & Workflows

### 🌐 UI Interaction
1. **Home Screen**: Open `http://localhost:8080/` in a browser.
2. **Registration / Login**: Click **Get Started** to sign up (`/login`).
3. **Dashboard**: Navigate to your dashboard, paste target URLs, set custom expiration dates, and view analytics charts.

### ⚙️ Command Line REST Workflows (cURL Examples)

#### 1. Register a New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "StrongPassword123"
  }'
```

#### 2. Authenticate & Obtain Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "StrongPassword123"
  }'
```
*Save the returned token string for authorization headers in subsequent requests.*

#### 3. Shorten a URL (Authenticated)
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "longUrl": "https://www.deepmind.google/technologies/gemini/",
    "expiresIn": 24
  }'
```

#### 4. Access the Redirect Endpoint
Paste the generated short link in your browser or run:
```bash
curl -i http://localhost:8080/<SHORT_CODE>
```
*Expect a `302 Found` response containing the original target in the `Location` header.*

---

## 📡 API Documentation

### Authentication Envelopes

#### `POST /api/auth/register` (Register User)
- **Request Body**:
  ```json
  {
    "name": "User Name",
    "email": "user@example.com",
    "password": "password"
  }
  ```
- **Response Structure (`200 OK`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "user@example.com",
    "name": "User Name",
    "role": "USER",
    "expiresAt": 1782755600000
  }
  ```

#### `POST /api/auth/login` (User Login)
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "password"
  }
  ```
- **Response Structure (`200 OK`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "email": "user@example.com",
    "name": "User Name",
    "role": "USER",
    "expiresAt": 1782755600000
  }
  ```

---

### Core URL Management

#### `POST /api/shorten` (Create Short Link)
*Requires Authorization header.*
- **Request Body**:
  ```json
  {
    "longUrl": "https://example.com/deep/nested/page/path",
    "expiresIn": 48,
    "expiresAt": "2026-07-05T12:00:00"
  }
  ```
- **Response Structure (`201 Created`)**:
  ```json
  {
    "shortUrl": "http://localhost:8080/a9b8c7",
    "shortCode": "a9b8c7",
    "longUrl": "https://example.com/deep/nested/page/path",
    "clickCount": 0,
    "createdAt": "2026-06-29T16:03:00",
    "expiresAt": "2026-07-01T16:03:00"
  }
  ```

#### `GET /api/expand/{shortCode}` (Resolve Short Code details)
- **Response Structure (`200 OK`)**:
  ```json
  {
    "shortCode": "a9b8c7",
    "longUrl": "https://example.com/deep/nested/page/path",
    "success": true,
    "message": "URL expanded successfully",
    "expiresAt": "2026-07-01T16:03:00"
  }
  ```

#### `GET /api/user/urls` (Fetch Current User's Links)
*Requires Authorization header.*
- **Response Structure (`200 OK`)**:
  ```json
  [
    {
      "id": 1,
      "longUrl": "https://example.com",
      "shortCode": "xyz123",
      "createdAt": "2026-06-29T12:00:00",
      "expiresAt": "2026-07-30T12:00:00",
      "clickCount": 42
    }
  ]
  ```

#### `GET /api/analytics/{shortCode}` (Access Click Analytics Data)
- **Response Structure (`200 OK`)**:
  ```json
  {
    "shortCode": "xyz123",
    "longUrl": "https://example.com",
    "totalClicks": 1250,
    "uniqueVisitors": 980,
    "clicksLast7Days": 340,
    "changePercentage": 12.5,
    "dailyClicks": [
      { "date": "2026-06-28", "count": 48 },
      { "date": "2026-06-29", "count": 65 }
    ],
    "topCountries": [
      { "country": "United States", "count": 600, "percentage": 48.0 },
      { "country": "Germany", "count": 250, "percentage": 20.0 }
    ],
    "deviceBreakdown": {
      "Desktop": 820,
      "Mobile": 380,
      "Tablet": 50
    }
  }
  ```

---

## 📂 Project Structure

```
SnipURL/
├── pom.xml                               # Maven dependencies & build metadata
├── Dockerfile                            # Docker image multi-stage build instructions
├── Jenkinsfile                           # Pipeline orchestration scripts for CI/CD
├── mvnw                                  # Maven wrapper executable (Unix)
├── mvnw.cmd                              # Maven wrapper script (Windows)
├── src/
│   ├── main/
│   │   ├── java/com/url_shortner/SnipURL/
│   │   │   ├── SnipUrlApplication.java   # Core execution entry point
│   │   │   ├── config/                   # Configs (Redis, Security, Cache)
│   │   │   ├── controller/               # API endpoints & web view routers
│   │   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── entity/                   # JPA mapping entities (Users, URLs, Clicks)
│   │   │   ├── exception/                # Standardized exception handler frameworks
│   │   │   ├── interceptor/              # Logging, Rate Limiter, and filter interceptors
│   │   │   ├── repository/               # JPA repositories mapping to Postgres
│   │   │   ├── security/                 # JWT helper utilities and User Details builders
│   │   │   └── service/                  # Business layers (Shortening logic, Analytics, GeoIP)
│   │   └── resources/
│   │       ├── application.properties    # Base properties configuration
│   │       ├── templates/                # Thymeleaf Views (Dashboard, Home, Login)
│   │       └── static/                   # Assets (Visual components, CSS files, custom scripts)
│   └── test/                             # Unit & Integration tests
```

---

## 💻 Development & Contribution Guide

We welcome contributions to SnipURL. Follow these guidelines to set up your environment:

### Setting Up a Development Workspace
1. **Lombok Config**: Ensure you enable Annotation Processing in your preferred IDE (IntelliJ IDEA, Eclipse, VS Code).
2. **Local Services**: Start local containers for postgres and redis using `docker-compose up -d`.
3. **Database Migration**: The application auto-migrates database schemas on startup (`spring.jpa.hibernate.ddl-auto=update`).

### Running Tests
Execute unit and integration tests using:
```bash
./mvnw test
```

### Static Analysis & Formatting
Run verify targets to execute plugins checkstyles:
```bash
./mvnw clean verify
```

---

## 🐛 Troubleshooting

### 1. Redis Connection Times Out
* **Symptom**: Application fails to boot, printing connection error messages like `RedisConnectionFailureException`.
* **Fix**: Ensure your Redis instance is running locally using `docker ps`. If running inside Docker, verify you configured `REDIS_HOST` correctly to point to `host.docker.internal` instead of `localhost` when the containerized application communicates with host-bound ports.

### 2. PostgreSQL Port 5432 Already Bound
* **Symptom**: Postgres container fails to start, displaying `port is already allocated` errors.
* **Fix**: Stop any existing PostgreSQL services running natively on your host machine:
  ```bash
  sudo systemctl stop postgresql
  ```

### 3. JWT Secret Validation Fails
* **Symptom**: App logs warn of unsafe key sizes: `WeakKeyException: The signing key's size is 128 bits...`.
* **Fix**: Supply a 256-bit cryptographically strong secret token in `JWT_SECRET`. You can generate a valid key using:
  ```bash
  openssl rand -hex 32
  ```

---

## ⚡ Performance & Optimization

To prepare the application for heavy production environments:
1. **Database Indexes**: Index the `url_mappings` table on the `short_code` field to support quick database lookup fallbacks.
2. **Lettuce Connection Pooling**: Customize pooling parameters (`spring.data.redis.lettuce.pool.max-active`) to match concurrent application requests.
3. **JVM Configuration**: Set appropriate memory constraints for container operations:
   ```bash
   java -XX:+UseG1GC -Xmx1g -Xms512m -jar app.jar
   ```

---

## 🔒 Security Best Practices

- **Strict JWT Token Lifetimes**: Configure shorter lifetimes (e.g., 30 minutes) and implement Refresh Tokens.
- **Encrypt Secrets**: Do not check-in database credentials. Use Docker secrets or Environment variable overrides.
- **HTTPS Only**: Enforce TLS termination at reverse proxies (Nginx / Cloudflare) to encrypt JWT tokens in transit.
- **BCrypt Password Encoding**: User passwords are automatically hashed with BCrypt. Do not weaken raw hashing parameters.

---

## 🗺️ Project Roadmap

- [ ] **Dynamic QR Code Generators** automatically created for every shortened URL.
- [ ] **Custom Domain Management** enabling enterprise accounts to tie their own subdomains.
- [ ] **Distributed Rate-Limiting** utilizing Redis clusters to support global endpoints.
- [ ] **Bulk URL Export** permitting CSV imports and bulk downloads.

---

## 🤝 Contributing Guidelines

1. Fork the Project Repository.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.

---

## 💡 Acknowledgments

- [Spring Boot documentation](https://spring.io/projects/spring-boot)
- [MaxMind GeoIP2 Web Services & Databases](https://www.maxmind.com)
- [Bucket4j Rate Limiting Library](https://github.com/bucket4j/bucket4j)
- All the open source contributors that built and maintained our base dependencies.
