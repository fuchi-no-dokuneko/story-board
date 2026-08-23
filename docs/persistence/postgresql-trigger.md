# PostgreSQL migration trigger

Status: DEFERRED

PostgreSQL is intentionally not implemented in v1. ADR-320 permits work to
begin only after measured evidence shows one of these conditions:

- sustained SQLite writer-lock pressure violates the two-commit-per-second SLO;
- more than one API writer host is required;
- database-enforced row-level security becomes mandatory;
- HA, PITR, or distributed job claiming becomes an approved requirement.

The 2026-08-23 qualification recorded 44.424 commits per second with 100 of 100
rows durable, no uncontrolled busy failures, and passing crash tests at all
eight commit boundaries. One API writer host remains the approved topology;
database RLS, HA/PITR, and distributed job claiming are not approved
requirements. Therefore no trigger is currently recorded and implementation
must remain absent.

When a trigger is approved, its evidence, owner, and date must first replace the
`not_triggered` state in `postgresql-trigger.json`. Migration then proceeds
offline through canonical export/import: stop writers, export every novel,
import into the PostgreSQL adapter, run the shared `RevisionStore` contract
suite, replay every revision, compare every head and render hash, and only then
move traffic. Optional RLS, distributed job claiming, HA, and PITR are added
only when named by the approved trigger. Dual writes remain prohibited.

The post-migration contract and head-hash acceptance criterion is conditional:
it becomes executable only after a trigger is approved and an adapter exists.
Until then, `scripts/verify-postgresql-gate.sh` proves the required outcome is
deferment without PostgreSQL production dependencies.
