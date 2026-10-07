# Launch operations — October 7

Target: public launch within two days. Live app: https://teamforge-jq44.onrender.com. Support/privacy contact: muhammadhassamir888@gmail.com.

## Report review

Reports now have a private paginated queue, review notes, reviewer account ID, time, revision, and outcome. An authorized moderator can dismiss a report or close its match. Closing revokes messaging for both members; project membership remains separate. Reviews cannot be overwritten through retries or stale requests. Reports remain subject to the existing account deletion cascade.

Access is disabled by default. Set `TEAMFORGE_MODERATOR_IDS` in the application's private environment to the comma-separated UUIDs of explicitly approved existing accounts. Obtain the owner's ID from their authenticated `/api/auth/me`, verify that the owner controls that account, and get approval before granting access. Do not grant access by email: email verification is unavailable. `Review reports` then appears in that account's navigation. Do not expose credentials or share report details publicly.

Owner action still needed: confirm daily review responsibility, identify the owner's account, and authorize its moderator access. There are no automatic report email alerts, bans, or project removal actions. Review the queue at least daily, record a reason, and use support email for follow-up. Do not advertise staffed review until this is established.

## Encrypted backups and restore checks

The repository supplies `scripts/database-backup.ps1` for PostgreSQL custom-format dumps, authenticated AES-256-GCM encryption, 14-day local retention, and isolated restore checks. `protect-backup.ps1` uses PBKDF2-SHA256 with 600,000 iterations and a random salt/nonce. Corruption and incorrect passphrases fail without creating a plaintext output. Plaintext temporary dumps are removed in a finally block; interruption or power loss can leave a temporary file, so use a trusted encrypted machine.

Requires PowerShell 7.4+ and official PostgreSQL 16 client tools. Set `PGHOST`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, `PGSSLMODE=require`, and `TEAMFORGE_BACKUP_PASSPHRASE` privately. Use the direct Neon endpoint for backup operations. Store a unique strong passphrase separately from the encrypted backup and credentials; never put it in source control, command arguments, CI logs, or this document.

Run `./scripts/database-backup.ps1` daily and before schema changes, using an encrypted off-site destination with `-Directory` if configured. Default output is ignored `.local/backups`. Encrypted files can be copied to an owner-controlled backup destination. Local retention only covers the selected directory; configure destination retention separately. A successful backup prints its file path without secrets.

To test recovery, first create a separate empty PostgreSQL database named `teamforge_restore_<suffix>`. Point the private PG environment at that target, then run `./scripts/database-backup.ps1 -Mode RestoreCheck -BackupPath <encrypted-file>`. The script refuses an existing/nonempty target and never drops a database or clears tables. It verifies migration history and reads accounts, tasks, and messages after restoring. This is a drill, not an automatic production failover. A failed restore can leave partial data only in the isolated target; inspect it before a retry.

CI encrypts an isolated PostgreSQL dump, decrypts it, compares bytes, restores it, and checks the schema. CI also tests incorrect-passphrase and tamper rejection. CI fixtures contain no production credentials or user data.

Production setup still pending: securely configure the client tools and credentials, choose a backup destination/passphrase, run the first real backup and isolated restore drill, and establish daily execution. These scripts alone do not mean production backups are running. A daily job on the owner's computer only runs while that machine is available; confirm an execution environment before claiming scheduled coverage.

## Release acceptance

`python scripts/acceptance-smoke.py https://teamforge-jq44.onrender.com` creates exactly two uniquely named disposable accounts. It only interacts with those account IDs, checks ranking/privacy, mutual consent, message retries, read state, coffee plans, projects/tasks, stale writes, report restrictions, export, blocking, recovery and session revocation, then deletes its fixtures. Never substitute real credentials or run destructive backend test suites against production.

Remaining walkthrough: actual phone and desktop signup, discovery swipes, conversations/projects, keyboard accessibility, and Help/privacy. Ask a second person to try the app and report errors before opening general signup promotion. Free Render can sleep and takes time to start; this remains a single-instance service with restart-expiring sessions. Preserve a rollback commit and deploy via the existing GitHub workflow.

## Go/no-go

- CI passes for the release commit, including PostgreSQL migrations and encrypted restore checks.
- Live acceptance passes and disposable fixture cleanup succeeds.
- Owner confirms report responsibility and moderator access works.
- Production encrypted backup is saved and restored into an isolated database; daily execution is established.
- Support email is published and monitored; privacy text matches storage, generated profiles, account deletion, and actual operations.
- Final mobile/desktop walkthrough and a second-person trial pass.

Keep status as preview until these checks are verified. A functioning app is not evidence that operational gates are complete. No trained model is required for this launch: ship the existing explained rules-based matching and avoid unsupported AI performance claims.
