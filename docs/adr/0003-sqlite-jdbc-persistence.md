# ADR 0003: SQLite JDBC Persistence

- Status: Accepted
- Date: 2026-08-21
- Owner: StoryBlock local implementation

## Decision

The first production adapter uses Xerial SQLite JDBC with Spring JDBC and
explicit SQL. Every connection enables WAL, `synchronous=FULL`, foreign keys,
a 5-second busy timeout, and explicit read-only transactions. The API is the
single database writer; expensive analysis occurs outside database transactions.
The pool starts at four physical connections and validates the required pragmas
when each connection is created. Loadable SQLite extensions remain disabled.

Flyway owns migrations. Checkpoints are verified replay caches rather than an
independent canonical source.

## Consequences

JPA and network-mounted SQLite files are excluded. PostgreSQL work begins only
after a measured migration trigger is documented. Returning a connection to the
pool must clear read-only state before it can be used for a write transaction.
