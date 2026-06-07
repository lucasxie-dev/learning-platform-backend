# Learning Platform Backend

A Spring Boot backend for an online learning platform. The project includes
course management, lesson management, role-based access control, learning
progress tracking, media uploads, signed file access URLs, dashboard summaries,
and API documentation.

This repository is designed as a practical backend portfolio project. It shows
how to structure a production-style Java service with authentication,
authorization, persistence, file storage, database migrations, and operational
configuration.

## Online Demo

Demo frontend:

```text
https://learning-platform-web.pages.dev/login
```

Demo accounts:

```text
admin@example.com
teacher@example.com
student@example.com
```

Demo password for all three accounts:

```text
ChangeMe123!
```

These credentials are for the public demo environment only. Do not reuse them in
production or in private deployments. Demo data may be reset periodically.

## Features

- JWT-based authentication with access and refresh tokens
- Role-based authorization for ADMIN, TEACHER, and STUDENT users
- Course and lesson creation, editing, publishing, ordering, and deletion
- Student enrollment and lesson progress tracking
- Admin and teacher dashboard APIs
- File upload management for course covers, lesson audio, lesson video,
  subtitles, and attachments
- Signed short-lived file access URLs for browser media playback
- PostgreSQL persistence with Flyway migrations
- Redis-backed caching
- Swagger/OpenAPI documentation for local development
- Centralized API response and exception handling

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Redis
- Flyway
- Maven
- Docker Compose for local infrastructure

## Project Structure

```text
src/main/java/dev/lucasxie/learning
├── auth        Authentication, JWT, and security user loading
├── course      Course domain, APIs, services, and mapping
├── lesson      Lesson domain, APIs, services, and mapping
├── progress    Enrollment and lesson progress workflows
├── file        File asset APIs, signed URLs, and media binding
├── storage     Storage abstraction and database-backed file storage
├── dashboard   Dashboard summary APIs
├── settings    Safe runtime settings overview API
├── user        User account APIs and queries
├── role        Role model and repositories
├── permission  Permission model and repositories
├── cache       Redis cache configuration
└── common      Shared API responses, exceptions, and base entities
```

## Local Quick Start

Start PostgreSQL, Redis, and MinIO:

```bash
docker compose up -d
```

Set a local JWT secret:

```bash
export JWT_SECRET=learning-platform-local-jwt-secret-must-be-at-least-32-bytes
```

Run the application:

```bash
./mvnw spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

If you use locally installed PostgreSQL, Redis, or MinIO instead of the project
`docker-compose.yml`, disable Spring Boot Docker Compose integration:

```bash
export SPRING_DOCKER_COMPOSE_ENABLED=false
```

For IntelliJ IDEA, add these values to the run configuration environment
variables.

## API Documentation

After the application starts locally, open Swagger UI at:

```text
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Bootstrap Demo Users

After Flyway has created the schema and seeded roles/permissions, you can create
local bootstrap users with:

```bash
docker compose exec -T postgres psql -U learning_user -d learning_platform < scripts/sql/init-local-users.sql
```

The script creates:

```text
admin@example.com
teacher@example.com
student@example.com
```

The local-only initial password is:

```text
ChangeMe123!
```

Change these passwords immediately after first login. Do not run the script
unchanged in production.

## Core API Areas

Settings overview:

```text
GET /api/v1/settings/overview
```

Admin and teacher global lists:

```text
GET /api/v1/lessons
GET /api/v1/files
```

Example authenticated requests:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/lessons?page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/files?assetType=LESSON_AUDIO&page=0&size=20"
```

## File Storage

By default, uploaded files are stored in PostgreSQL for easier local
development, demo setup, and small files. The Docker Compose file also includes
MinIO for future object-storage work.

The protected file content endpoint requires a Bearer token:

```text
GET /api/v1/files/{fileId}/content
```

Browser media elements such as `img`, `audio`, and `video` should use a signed
access URL from:

```text
POST /api/v1/files/{fileId}/access-url
```

Signed URLs are short-lived and can be used directly as media `src` values
without an Authorization header.

Signed file access requires a secret for HMAC SHA-256 token generation. For
local development, the application has a non-production default. For production,
set a strong random value:

```bash
export FILE_ACCESS_SECRET=learning-platform-local-file-access-secret-change-me
```

Do not reuse the local example value in production. You can also tune signed URL
lifetimes with:

```bash
export FILE_ACCESS_DEFAULT_EXPIRATION_MINUTES=15
```

For production-like deployments or larger media files, switch the storage layer
to object storage by adding an S3 or MinIO provider implementation and setting
the corresponding credentials.

## Configuration

Common environment variables:

```bash
export SERVER_PORT=8080
export SPRING_DOCKER_COMPOSE_ENABLED=false

export DB_HOST=127.0.0.1
export DB_PORT=5432
export DB_NAME=learning_platform
export DB_USERNAME=learning_user
export DB_PASSWORD=learning_password

export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=

export JWT_SECRET=replace-with-a-strong-random-secret-at-least-32-bytes
export FILE_ACCESS_SECRET=replace-with-a-strong-random-secret

export STORAGE_PROVIDER=database
export STORAGE_MAX_FILE_SIZE_MB=20
export STORAGE_PUBLIC_BASE_URL=http://localhost:8080
```

The local development services use non-production credentials defined in
`docker-compose.yml`. Use strong random secrets and private database credentials
in production.

## Production Notes

SpringDoc exposes `/v3/api-docs` and `/swagger-ui/index.html` by default. If API
documentation should not be publicly available in production, disable it with:

```bash
export SPRINGDOC_API_DOCS_ENABLED=false
export SPRINGDOC_SWAGGER_UI_ENABLED=false
```

Recommended deployment hardening:

- Use HTTPS in front of the service
- Keep PostgreSQL and Redis off the public internet
- Set strong values for `JWT_SECRET` and `FILE_ACCESS_SECRET`
- Limit upload size at both the application and reverse proxy layers
- Add rate limiting for login, registration, and upload endpoints
- Disable public registration for controlled demos
- Disable public Swagger/OpenAPI documentation in production

## Related Repository

Frontend repository:

```text
https://github.com/lucasxie-dev/learning-platform-web
```
