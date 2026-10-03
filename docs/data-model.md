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

Flyway V4 adds `coffee_proposals`, scoped to a real match and proposer, with unique per-proposer client request IDs, UTC start time, IANA timezone, kind, note, status, and creation time. Match locking serializes proposals and responses against unmatching. These records are retained with the conversation.

Flyway V5 adds projects with owner, idempotency client ID, name, description, stage and creation time; project_members links invitees with INVITED/ACCEPTED/DECLINED status. Ownership is implicit membership. Project locking serializes invitation caps and responses; account locking serializes per-owner creation caps. Foreign keys cascade on account/project deletion. These tables contain real-user data only; demo projects remain temporary browser state.

V6 adds an optimistic revision to project details and an ended_reason to member history. Leaving/removal sets underlying status DECLINED to revoke access, with LEFT/REMOVED presented in the roster. Records remain stored; no destructive deletion is involved. All roster reads, edits, and membership changes serialize on the project row.

V7 adds directional account_blocks and private safety_reports. Block uniqueness is actor/target; report uniqueness is reporter/client UUID. Reports retain match reference, reason, details and creation time. Account/match foreign keys cascade. Blocking closes a match without deleting messages or proposals. The reporter's own records appear in export; other people's reports do not.
