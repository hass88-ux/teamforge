# Ten-day delivery plan

Planning window: September 30 through October 10, 2026, with the October 10 delivery deadline confirmed.

1. Foundation: reproducible builds, architecture, CI, local setup.
2. Profiles: migrations, validated profile contract, interactive onboarding.
3. Demo: diverse fictional profiles, isolated sessions, scoring and explanations.
4. Discovery: like/pass, reciprocal matches, matched profiles and projects.
5. Collaboration: authorized messaging and coffee-chat proposals.
6. Real accounts: registration, sessions, ownership, privacy controls.
7. Intelligence: synthetic evaluation with correct splits, ML baseline comparison.
8. Teams: project roles, constrained team selection, optional GitHub OAuth.
9. Release: current free-tier research, deployment, end-to-end and security checks.
10. Polish: responsive/accessibility review, screenshots, honest portfolio documentation.

Each milestone must build, pass relevant tests, update documentation, and become a coherent commit. This is a prioritized delivery plan, not a claim that every research model can be validated in ten days. Advanced models must earn inclusion through measured evaluation. Synthetic metrics remain labeled synthetic.

## External dependencies

- GitHub repository URL and authenticated push access.
- Hosting account setup and production database credentials when deployment begins.
- GitHub OAuth application credentials for optional account enrichment.

## Usage discipline

Use focused changes and targeted tests. Avoid unnecessary model inference, redundant repository scans, fabricated microcommits, and repeated unchanged status checks. Check account limits at major milestones. No purchased credits or paid services are assumed.

## Current milestone and next priorities

Local demo, accounts, profile persistence, and opt-in real discovery are implemented. Persistent likes/passes, mutual matches, and matched-user messaging are implemented. Real coffee invitations are implemented. Real saved projects and consent-based team invitations are implemented. Project editing and leave/removal controls are implemented. Next: optional GitHub enrichment; a deployable beta with PostgreSQL and HTTPS; email recovery, moderation operations, privacy notice; responsive/accessibility polish and honest evaluation. These remain release work, not completed features.

Description-based skill suggestions are implemented as a reviewed English rule-based baseline. Next intelligence work: measured model evaluation and optional GitHub enrichment; no trained-model claim is made.

Offline synthetic model evaluation is implemented with profile-disjoint train/validation/test groups. The trained logistic baseline failed to improve held-out ranking and was not promoted. Remaining intelligence work needs better training data and fresh evaluation before production adoption.

Account data download is implemented with owner-only fields, no-cache responses, and isolation coverage. Recovery keys and account deletion are implemented. Email recovery, moderation operations, retention policy, and public hosting remain release work.

Blocking and private report recording are implemented. Public-release safety still needs a functioning moderation workflow, email recovery, retention policy, and production deployment verification.

October 5 batch: mobile/keyboard polish, in-app Help & privacy, and integrated frontend/API release packaging are implemented. CI now builds the release JAR as a downloadable artifact. Public hosting, PostgreSQL integration, HTTPS checks, moderation operations, optional OAuth, and final beta acceptance remain unfinished. The in-app local privacy explanation is not a production public privacy/contact policy.

## Milestone status as of October 5

Eight of ten milestones have their core scope completed locally; release and final polish remain in progress. This count does not claim optional OAuth or a publicly launched beta.

| Milestone | Status | Evidence or remaining work |
| --- | --- | --- |
| 1 Foundation | Complete | Repository, CI, reproducible setup |
| 2 Profiles | Complete | Validated onboarding and migrations |
| 3 Demo | Complete | 600 sample profiles and isolated demo state |
| 4 Discovery | Complete | Intent rules, scoring, real mutual matching |
| 5 Collaboration | Complete | Authorized messages and coffee proposals |
| 6 Real accounts | Complete | Sessions, ownership, privacy, recovery keys, deletion |
| 7 Intelligence baseline | Complete | Profile-disjoint synthetic evaluation; failed learned baseline not promoted |
| 8 Teams core | Complete | Saved projects, consent-based invitations, tasks, demo team optimization; optional OAuth and automatic real-team ranking remain future work |
| 9 Release | In progress | Packaged app and repeatable smoke check verified; hosting, PostgreSQL, HTTPS, backup/restore and moderation operations remain |
| 10 Final polish | In progress | Mobile/help/keyboard improvements and screenshots; broader device and final beta acceptance review remain |

The read-only smoke check passes against both Vite development and the bundled frontend on the local API. It verifies service health, release assets, and unauthenticated route protection without creating accounts or changing data.

October 5 PostgreSQL checkpoint: the complete backend suite passed against an isolated PostgreSQL 16 service in GitHub Actions (commit 2237154). All eight Flyway migrations and the existing application tests ran successfully. See [PostgreSQL verification](postgresql.md) for database isolation and backup scope. Public hosting, provider credentials and production HTTPS remain unfinished.
