# PostgreSQL migration trigger

PostgreSQL is intentionally not implemented in v1. ADR-320 permits work to
begin only after measured evidence shows one of these conditions:

- sustained SQLite writer-lock pressure violates the two-commit-per-second SLO;
- more than one API writer host is required;
- database-enforced row-level security becomes mandatory;
- HA, PITR, or distributed job claiming becomes an approved requirement.

No trigger is currently recorded. When one is approved, migration must use the
canonical export/import contracts and verify every replayed novel head hash
before traffic moves. Dual writes remain prohibited.
