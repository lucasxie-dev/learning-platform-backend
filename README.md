# Learning Platform Backend

## Local Infrastructure

Start PostgreSQL, Redis, and MinIO:

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

SpringDoc exposes `/v3/api-docs` and `/swagger-ui.html` by default. If API
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
