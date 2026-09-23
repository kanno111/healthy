# Database migrations

Flyway owns the schema in this directory.

- `V1` through `V8` are the historical incremental migrations.
- `B8__baseline_schema.sql` is the complete schema snapshot for a new empty MySQL 8 database.
- An existing pre-Flyway database is baselined at version `8`, so neither `B8` nor `V1` through `V8` is executed there.
- `V9` and every later `V` migration are applied to both baselined databases and new databases.

After a migration has been applied anywhere, do not rename, delete, reorder, or edit it. Add a new migration with the next version instead.

`baseline-on-migrate` is disabled by default. The current development database has already been baselined. For another existing database, enable it only for the first controlled startup with `FLYWAY_BASELINE_ON_MIGRATE=true`, verify the schema first, and disable it again afterwards.

Do not put demo patients, appointments, passwords, tokens, or other business data in schema migrations.
