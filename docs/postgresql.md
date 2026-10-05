# PostgreSQL verification

The GitHub Actions `postgres` job creates an isolated PostgreSQL 16 service and runs the complete Java test suite with JDBC overrides. Flyway applies all eight migrations and Hibernate validates the schema. Existing tests exercise sessions, profiles, reciprocal matching, authorization, messaging, coffee proposals, projects/tasks, recovery, export, blocking and deletion on that database.

The CI username/password are disposable test-only credentials, not deployment credentials. The job uploads Surefire reports for seven days, including on failure. Tests delete/reset fixtures: never point this suite at a production database or a database containing data you want to keep.

After successful tests, CI creates a custom-format backup using the PostgreSQL container's own pg_dump, restores it into a second isolated database, and checks the migration count and representative tables. This proves the test database is restorable; it does not implement scheduled production backups or off-site storage.

Production still requires a separate database, private credentials, encrypted connections according to the hosting provider, restricted network access, backups with an explicit retention policy, and an actual restore drill using the configured provider. Public deployment must verify Secure cookies and same-origin API access over HTTPS. The local H2 database stays unchanged while PostgreSQL validation runs in CI.

No automatic H2-to-PostgreSQL transfer is implemented. Local QA records should not be treated as launch users. A fresh beta database is the default; transferring real local records would require a separate reviewed migration.

Verified: PostgreSQL suite succeeded for commit 2237154 in [GitHub Actions](https://github.com/hass88-ux/teamforge/actions/runs/37269918989). Backup/restore verification is added in the following commit and must pass separately before being reported as verified.
