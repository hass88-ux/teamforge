# Beta acceptance checklist

Use test accounts and sample content. Do not delete real accounts or change real credentials during a walkthrough.

Verified automated/local baseline:

- Java suite passes on H2 and PostgreSQL 16; all eight migrations apply.
- Isolated PostgreSQL backup restores and representative tables are readable.
- Frontend state tests, lint and production build pass.
- Anonymous smoke checks verify service health and private-route protection.
- Local two-account workflows cover mutual consent, messages, coffee plans and project membership.

Final walkthrough to repeat before release:

- Demo onboarding, offered/needed skills and provider/seeker restrictions; scored recommendations and clearly labeled simulated replies.
- Real signup/login, saved profile, private default, opt-in discovery and full profile display.
- Two reciprocal likes, authorized messaging, unread count after visible reading, and navigation back to the correct origin.
- Coffee invitation proposal, recipient response and upcoming dashboard entry.
- Project invitation acceptance, task completion, stale revision rejection and membership removal/leave.
- Export, private recovery key, session revocation, blocks and account deletion using disposable test fixtures only.
- Narrow mobile and desktop layouts, keyboard navigation, focus indicators, Help dialog dismissal and long-text wrapping.

Public beta gates still pending:

- Free hosting/database limits verified and acceptable, with no payment commitment.
- Deployed PostgreSQL credentials and private recommendation routing configured.
- HTTPS, Secure cookies, same-origin assets/API and restart behavior checked in the deployed environment.
- Production backup storage, retention and restore drill established.
- Report-review responsibility and public privacy/contact information established.

Passing CI is evidence for the tested workflows, not proof that these public beta gates are complete.
