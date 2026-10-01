# Persistence model

Flyway migration V1 creates:

- `accounts`: UUID identity, unique normalized email, BCrypt hash, REAL-only account type constraint, created timestamp.
- `profiles`: account UUID primary/foreign key, validated profile JSON, update timestamp, optimistic row version.

Full profiles remain owner-scoped; public discovery fields are shared only after explicit visibility opt-in. Validated JSON preserves the current onboarding contract while avoiding premature normalization of the evolving taxonomy. Skills, interests, projects, interactions, matches, and messages will need relational structures as persisted real collaboration is implemented. Database migrations, not Hibernate auto-update, own schema changes; Hibernate currently validates the schema at startup.

Local development uses file-backed H2 in PostgreSQL compatibility mode under `.local/teamforge.mv.db`, excluded from Git. Tests use a separate in-memory database and exercise migrations. Public demo profiles remain synthetic Python data; demo interactions remain browser state and never enter account/profile tables.

Production configuration uses the PostgreSQL driver with environment-provided connection details. A PostgreSQL server has not yet been exercised; H2 compatibility mode does not substitute for a PostgreSQL migration/integration test. No claim of verified PostgreSQL deployment is made.

Profiles survive reload and backend restart. In-memory login sessions do not survive backend restart, so the user signs in again to recover saved profile data. The default local database is for development, not a production storage or backup strategy.

Flyway V2 adds `profiles.discoverable`, default false, so existing saved accounts stay private. Visibility is owned by the same account UUID and survives profile edits. No demo record is inserted into these tables.

Flyway V3 adds `profile_decisions` (actor/target composite key), `collaboration_matches` (unique canonical member pair, score at match, creation/closure timestamps), and `collaboration_messages` (database sequence, match, server-derived sender, client request UUID, bounded body, timestamp). Foreign keys bind these records to real accounts; demo never writes them. Closed matches retain history but the application denies access and future contact. Database-backed account deletion would cascade, but no user-facing deletion endpoint exists yet.
