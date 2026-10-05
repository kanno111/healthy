# Identity database migrations

`healthy-identity` exclusively owns `sys_user` and `patient` in the
`healthy_identity` database. Other services retain only logical IDs and must
query identity data through the internal HTTP API.

When upgrading an existing installation, run `scripts/db/copy-identity-data.sql`
and confirm every mismatch count is zero before allowing `healthy-server` to
apply its migration that removes the old tables.
