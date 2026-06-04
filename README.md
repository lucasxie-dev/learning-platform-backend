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

The local development services use non-production credentials defined in `docker-compose.yml`.
