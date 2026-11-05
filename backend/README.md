# HappenStance Backend — Phase 1

## Included
- Spring Boot REST backend (Java 25 LTS baseline)
- MongoDB collections/models: users, profiles, relationships
- Registration and login endpoints using BCrypt password hashes and signed JWT access tokens
- Protected `GET /api/v1/profiles/me`
- CORS configuration and Docker Compose

## Run locally (PowerShell)
1. Install JDK 25+ and Maven 3.9+, and ensure MongoDB is running.
2. Copy `.env.example` to `.env`; use a strong random JWT secret of at least 32 bytes.
3. From this directory:

```powershell
$env:JWT_SECRET = "replace-this-with-a-random-secret-at-least-32-bytes"
$env:MONGODB_URI = "mongodb://localhost:27017/happenstance"
mvn spring-boot:run
```

The secret is checked at startup. Never commit `.env` or a real secret.

## Run with Docker
Set `JWT_SECRET` in your shell or `.env`, then run `docker compose up --build`.

## Endpoints
- `GET /api/v1/health` — public health
- `POST /api/v1/auth/register` — JSON `{ "displayName": "Example User", "email": "user@example.com", "password": "at-least-12-characters" }`
- `POST /api/v1/auth/login` — JSON `{ "email": "user@example.com", "password": "at-least-12-characters" }`
- `GET /api/v1/profiles/me` — requires `Authorization: Bearer <accessToken>`

Registration creates an empty profile. Relationship records preserve provenance and visibility; do not treat inferred links as verified relationships.

## Important
This is a development baseline, not production-ready. Next add auth tests, rate limiting, email verification, password reset/revocation, profile update APIs, relationship consent flows, audit logs and production privacy controls. LLM and GNN are intentionally separate next milestones; they are not connected in this phase.
