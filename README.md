 # SnipURL - URL Shortener Service

SnipURL is a full-featured URL shortening service that converts long, cumbersome URLs into short, manageable links. Built with enterprise-grade technologies, it provides secure authentication, intelligent caching, and rate limiting.

---

## Features

| Feature | Description |
|---------|-------------|
| **URL Shortening** | Convert long URLs to short codes (custom or auto-generated) |
| **JWT Authentication** | Secure user registration and login |
| **Redis Caching** | Sub-millisecond URL resolution |
| **Rate Limiting** | Prevent API abuse |
| **Click Analytics** | Track total clicks and statistics |
| **URL Expiration** | Auto-expire URLs after set duration |
| **REST API** | Full programmatic access |
| **Docker Support** | One-command deployment |

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.x | Backend framework |
| Java | 17 | Programming language |
| PostgreSQL | 15 | Database |
| Redis | 7 | Caching |
| Maven | 3.9+ | Build tool |
| Docker | Latest | Containerization |
| Jenkins | Latest | CI/CD |

---

## Prerequisites

bash
# Required installations
Java 17
Maven 3.9+
Docker
Docker Compose
Git


 ## verify installations 
java --version
mvn --version
docker --version

## running application 

# Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=snipurl \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:15

# Start Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine

# Pull the latest image
docker pull vishwahubballi/snipurl:latest

# Run with PostgreSQL and Redis (using host.docker.internal)
docker run -d --name snipurl-app \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/snipurl \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=my-custom-secret-key \
  vishwahubballi/snipurl:latest

  # Alternative 
  # Create docker-compose.yml file with content below, then run:
    docker-compose up -d

    version: '3.8'
services:
  postgres:
    image: postgres:15
    container_name: snipurl-postgres
    environment:
      POSTGRES_DB: snipurl
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
  redis:
    image: redis:7-alpine
    container_name: snipurl-redis
    ports:
      - "6379:6379"
    restart: unless-stopped
  app:
    image: vishwahubballi/snipurl:latest
    container_name: snipurl-app
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/snipurl
      DB_USERNAME: postgres
      DB_PASSWORD: secret
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: your-super-secret-jwt-key-change-this
    restart: unless-stopped
volumes:
  postgres_data:   


  
## Access URLs
    Service	URL	Credentials
        Application API	http://localhost:8080	Create account via /api/auth/signup
        PostgreSQL	localhost:5432	postgres / secret
        Redis	localhost:6379	No password


## Environment Variables
    Variable	                        Default	Description
    DB_URL                    	jdbc:postgresql://localhost:5432/snipurl	PostgreSQL connection URL
    DB_USERNAME	                postgres	Database username
    DB_PASSWORD	                secret	Database password
    REDIS_HOST	                localhost	Redis host
    REDIS_PORT	                6379	Redis port
    JWT_SECRET	                (default)	JWT signing secret (CHANGE THIS)
    JWT_EXPIRATION              86400000	JWT expiration in ms (24 hours)
    CACHE_TTL	3600	            Cache TTL in seconds (1 hour)
    APP_BASE_URL	               http://localhost:8080	Application base URL





