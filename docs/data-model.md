# Persistence model

Flyway migration V1 creates:

- `accounts`: UUID identity, unique normalized email, BCrypt hash, REAL-only account type constraint, created timestamp.
- `profiles`: account UUID primary/foreign key, validated profile JSON, update timestamp, optimistic row version.

Profiles are private to their owner at this milestone. Validated JSON preserves the current onboarding contract while avoiding premature normalization of the evolving taxonomy. Skills, interests, projects, interactions, matches, and messages will need relational structures when real-user discovery/collaboration is implemented. Database migrations, not Hibernate auto-update, own schema changes; Hibernate currently validates the schema at startup.

Local development uses file-backed H2 in PostgreSQL compatibility mode under `.local/teamforge.mv.db`, excluded from Git. Tests use a separate in-memory database and exercise migrations. Public demo profiles remain synthetic Python data; demo interactions remain browser state and never enter account/profile tables.

Production configuration uses the PostgreSQL driver with environment-provided connection details. A PostgreSQL server has not yet been exercised; H2 compatibility mode does not substitute for a PostgreSQL migration/integration test. No claim of verified PostgreSQL deployment is made.

Profiles survive reload and backend restart. In-memory login sessions do not survive backend restart, so the user signs in again to recover saved profile data. The default local database is for development, not a production storage or backup strategy.
