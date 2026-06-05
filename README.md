# Learning Platform Backend

## Local Infrastructure

Start PostgreSQL and Redis. The Docker Compose file also includes MinIO for
future object-storage work, but file uploads use PostgreSQL by default:

```bash
docker compose up -d
```

Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The application requires a JWT secret to issue access and refresh tokens. For
local development, set a value with at least 32 bytes:

```bash
JWT_SECRET=learning-platform-local-jwt-secret-must-be-at-least-32-bytes
```

If you use locally installed PostgreSQL, Redis, or MinIO instead of the
project `docker-compose.yml`, disable Spring Boot Docker Compose integration:

```bash
SPRING_DOCKER_COMPOSE_ENABLED=false
```

For IntelliJ IDEA, add this value to the run configuration environment variables.

The local development services use non-production credentials defined in `docker-compose.yml`.

Use a strong random `JWT_SECRET` in production. Do not reuse the local
development example value.

## API Documentation

After the application starts locally, open the Swagger UI at:

```text
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Settings Overview

The frontend Settings page can read safe profile, system, and media storage
metadata from:

```text
GET /api/v1/settings/overview
```

This endpoint requires a Bearer token for any authenticated user. It does not
expose secrets and does not allow changing runtime configuration.

## Admin Console Lists

The frontend admin console can read global, paged management lists for lessons
and media assets:

```text
GET /api/v1/lessons
GET /api/v1/files
```

Both endpoints require an ADMIN or TEACHER Bearer token. ADMIN users can list all
records. TEACHER users can list lessons under courses they own and media assets
they uploaded or assets attached to their courses/lessons.

Example requests:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/lessons?page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/lessons?status=PUBLISHED&page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/lessons?hasAudio=false&page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/files?page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/files?bound=false&page=0&size=20"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/files?assetType=LESSON_AUDIO&page=0&size=20"
```

## File Storage

By default, uploaded files are stored in PostgreSQL for easier local development,
demo setup, and small files. For production-like deployment or larger media
files, switch `app.storage.provider` to `minio` or `s3` after adding the
corresponding provider implementation and credentials.

The protected file content endpoint, `/api/v1/files/{fileId}/content`, requires
a Bearer token. Native browser media elements such as `img`, `audio`, and
`video` should use a signed access URL from
`POST /api/v1/files/{fileId}/access-url`. Signed URLs are short-lived and can be
used directly as media `src` values without an Authorization header.

Signed file access requires a secret for HMAC SHA-256 token generation. For
local development, the application has a non-production default. For production,
set a strong random value:

```bash
FILE_ACCESS_SECRET=learning-platform-local-file-access-secret-change-me
```

Do not reuse the local example value in production. You can also tune signed URL
lifetimes with:

```bash
FILE_ACCESS_DEFAULT_EXPIRATION_MINUTES=15
```

## Optional Local Bootstrap Users

After Flyway has created the schema and seeded roles/permissions, you can
create local bootstrap users with:

```bash
docker compose exec -T postgres psql -U learning_user -d learning_platform < scripts/sql/init-local-users.sql
```

The script creates `admin@example.com`, `teacher@example.com`, and
`student@example.com` with the local-only initial password `ChangeMe123!`.
Change these passwords immediately after first login. Do not run the script
unchanged in production.

## Production Notes

SpringDoc exposes `/v3/api-docs` and `/swagger-ui/index.html` by default. If API
documentation should not be publicly available in production, disable it with:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

For environment-based configuration, use:

```bash
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```
