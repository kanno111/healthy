# Doctor database migrations

Flyway in `healthy-doctor` exclusively owns the `healthy_doctor` schema.

- `V1` creates the final `department` and `doctor` tables for a new database.
- Existing business data is copied once by `scripts/db/copy-doctor-data.sql` after V1 has run.
- Runtime code must not read tables from the identity/booking database.
- Cross-context identifiers such as `doctor.user_id` remain logical IDs without foreign keys.

Applied migrations are immutable. Add a new versioned migration for every later schema change.
