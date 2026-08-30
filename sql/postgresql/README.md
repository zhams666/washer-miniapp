# CloudBase PostgreSQL Initialization

`001_cloudbase_init.sql` is the PostgreSQL equivalent of the MySQL migrations in `../migrations/`.

Use it only for a new, empty CloudBase PostgreSQL test environment:

1. Open the CloudBase environment's PostgreSQL SQL console.
2. Select the database/schema supplied by CloudBase.
3. Run `001_cloudbase_init.sql` once.
4. Copy the PostgreSQL host, port, database, username, and password into the CloudBase Run service variables `WASHER_PG_URL`, `WASHER_PG_USERNAME`, and `WASHER_PG_PASSWORD`.

Do not run this script against an environment that already contains production data. The original MySQL migration files remain unchanged for local MySQL development.

When MySQL migrations change, regenerate the PostgreSQL initialization script from the repository root:

```powershell
node scripts/convert-mysql-migrations-to-postgresql.mjs
```
