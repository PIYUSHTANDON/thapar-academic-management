# PostgreSQL through Docker

PostgreSQL 17 is the sole database used by the backend and all automated integration tests (H2 and MySQL have been removed).

## Start PostgreSQL

Start Docker Desktop, then run from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-postgres.ps1
```

The first run generates database passwords in the ignored `.env.postgres` file and creates both:
- `thapar_ams` (schema `academic`, owned by non-superuser role `ams`) for application data
- `thapar_ams_test` (owned by role `ams`) for isolated per-test-class schemas dropped automatically after tests finish

To also start the backend directly via Maven:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-postgres.ps1 -StartBackend
```

Or use the single-command Maven desktop launcher (reads `.env.postgres` automatically):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File mvnw.ps1 -Papp
```

Connection: `127.0.0.1:55432`, database `thapar_ams`, schema `academic`, application user `ams`. The password is `DB_PASSWORD` in `.env.postgres`. The database port is bound only to `127.0.0.1`. PostgreSQL data persists in the Compose `postgres-data` volume.

Flyway applies `backend/src/main/resources/db/postgresql/V1` through `V3` on backend startup. Binary attachments use PostgreSQL large objects (`oid`) to match JPA `@Lob`; file access remains inside service transactions.

To stop PostgreSQL while retaining its data:

```powershell
docker compose --env-file .env.postgres stop postgres
```

Backups should use `pg_dump` before any future database migration. Removing the Docker volume deletes its database.
