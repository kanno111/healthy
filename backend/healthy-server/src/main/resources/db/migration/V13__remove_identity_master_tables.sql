-- Identity accounts and patient profiles are now owned by healthy-identity in
-- the healthy_identity database. Booking retains patient IDs as logical
-- references only. Data must be copied and verified before this migration runs.

DROP TABLE IF EXISTS patient;
DROP TABLE IF EXISTS sys_user;
