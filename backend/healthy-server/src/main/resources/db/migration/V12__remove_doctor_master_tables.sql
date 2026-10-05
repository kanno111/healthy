-- Doctor and department master data is now owned by healthy-doctor in the
-- healthy_doctor database. Booking retains doctor IDs as logical references.
-- Data must be copied and verified before this migration is deployed.

DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS department;
