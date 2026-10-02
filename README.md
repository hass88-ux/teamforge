# TeamForge

Find the people you should build with.

TeamForge is being built as a collaborator matchmaking platform with structured, reciprocal recommendations. This repository has a working local demo and is still in development. There is no public deployment or real-user traction yet.

## Current implementation

- React + TypeScript landing page and six-step interactive demo onboarding (Vite).
- Java 21 Spring Boot API, Spring Security session authentication, CSRF protection, and health probes.
- Persistent owner-scoped profiles, BCrypt password hashing, and Flyway-managed accounts/profile tables.
- Python/FastAPI reciprocal weighted ranking with 600 diverse fictional profiles, grounded explanations, and disclosed tailored demo candidates.
- Discovery cards, synthetic project details, and temporary like/pass controls.
- Disclosed simulated mutual matches, scripted demo conversations, and coffee-chat proposals with simulated accept/decline controls.
- Temporary project creation and constrained team recommendations with inspectable scoring.
- Opt-in real-profile discovery with private-by-default visibility and the same intent-aware ranking.
- Persistent real likes/passes, genuine mutual matches, paginated matched-user messages, and unmatching.
- Architecture, CI, and ten-day delivery plan.

## Local development

Windows: after installing dependencies and building the backend, run `./scripts/start-local.ps1` from PowerShell to start all three services in hidden background processes. Open `http://127.0.0.1:5173/`. Logs are in `%TEMP%/teamforge-local`. This launcher uses the last built backend JAR; run Maven verify after backend changes before restarting it. Services remain local and do not create a public deployment.

Backend: install Java 21 and Maven, then run `mvn -f backend/pom.xml spring-boot:run`.
Health: `http://localhost:8080/actuator/health`.
Backend checks: `mvn -f backend/pom.xml verify`.
Frontend: run `npm ci` and `npm run dev` inside `frontend`.
Frontend checks: run `npm test`, `npm run lint`, and `npm run build` inside `frontend`.
AI service: follow [ai/README.md](ai/README.md) and start it on port 8001 before discovery. Start Java and Vite alongside it. The frontend development proxy forwards /api to Java on port 8080. Production needs same-origin API routing.

## Planned product

Interactive onboarding, isolated demo sessions, 600 demo profiles, explained reciprocal ranking, discovery, mutual matches, projects, messaging, coffee-chat proposals, and team optimization. Real-user authentication and owner-scoped profile storage work locally; optional public-only GitHub enrichment is planned. Demo onboarding and baseline discovery are implemented. persisted projects/interactions, trained models, and GitHub OAuth remain unimplemented. Accounts and owner-scoped profiles now persist locally. Demo team recommendations use a bounded beam-search heuristic; real-user team formation is not yet implemented. Demo matches and conversations are clearly labeled simulations.

See [architecture](docs/architecture.md), [demo system](docs/demo-system.md), [recommendation baseline](docs/recommendation-system.md), [team formation](docs/team-formation.md), and [delivery plan](docs/delivery-plan.md).

## Verification

Current local checks pass: 32 Java tests, 18 Python tests, 6 frontend state tests, frontend lint, and production build. CI runs these suites. Browser walkthroughs cover onboarding, scored discovery, fictional projects, simulated match/message/coffee chat, project creation, feasible team coverage, and an honest infeasible result.

A local two-account walkthrough verified compatibility, reciprocal likes, saved messages/replies, and recovered history after a backend restart. QA accounts are not real-user traction.

Real coffee invitations now support proposing, recipient accept/decline, cancellation, and persistent status within matched conversations. No calendar event, meeting link, or email is created.

No public deployment, real-user metrics, learned-model evaluation, or production security certification is claimed. Demo state is temporary and profiles use concise demo labels and automated replies are identified. See [security](docs/security.md), [privacy](docs/privacy.md), and [persistence](docs/data-model.md) for current guarantees and unfinished work.

## Local accounts

Signup and login use the Java backend and a local file-backed H2 database. Credentials and profile records are not committed. Sessions are held in server memory; a backend restart logs you out, while accounts/profiles remain on disk. Demo remains separate and temporary.

For future PostgreSQL deployment, activate Spring profile `production` and set `DATABASE_URL` to a JDBC PostgreSQL URL, `DATABASE_USER`, and `DATABASE_PASSWORD`. HTTPS is required because production cookies are Secure. PostgreSQL integration and public deployment have not been verified yet. Never commit these environment values.

### Intent-aware swipe discovery

The demo contains 600 deterministic fictional profiles, including individuals and organizations. Onboarding collects provider, seeker, or collaborator intent; editable descriptions; offered skills; and needed skills (including Node.js). Providers exclude providers, seekers exclude seekers, and collaborators can match any intent. Discovery shows mutual compatibility above 50% before a like, profile type, intent, and “No GitHub connected” for these unlinked profiles. Swipe left/right, use arrow keys on the card, or use Pass/Like buttons. A like simulates a match and opens the existing in-app demo messaging flow; fictional replies are labeled. Real discovery supports opted-in saved profiles, persistent likes/passes, mutual matches, and matched-user messaging.

Descriptions are editable profile text. Structured offered/needed skill selections drive the heuristic; free text is not yet interpreted by a language model. Existing saved profiles receive safe defaults for the new fields.

### Real-profile discovery

After signing in and saving a profile, choose **Discover people**. Profiles remain private by default, including existing accounts. Explicitly enable **Show my profile in discovery** to let signed-in members see your public collaboration fields; hide it again anytime. Email, password hashes, timezone, and selected availability slots are excluded from candidate responses. Real discovery ranks only opted-in stored profiles, filters self/incompatible intents/scores at or below 50%, and has no demo fallback. The initial retrieval pool is at most 200 recently updated visible profiles, returning up to 20 results. An empty network shows an honest empty state. Real likes, mutual matches, and authorized messaging are implemented.

### Real matches and conversations

Like or pass saves your choice. Likes require both profiles to be discoverable, compatible intents, and a freshly checked score above 50%. Only two genuine reciprocal likes create a match. Real conversations have no automated replies. Messages persist in the local database, are accessible only to the two active members, and refresh manually. Each send includes a client request UUID so retries do not create duplicates. Conversations page in batches of 50; sending is limited to 30 new messages per user per minute. Unmatching closes access for both members and prevents further contact for that pair in this preview; records remain stored, pending retention/deletion controls. Hiding discovery visibility does not close existing matches.

This milestone has not added live push notifications, read receipts, attachments, reporting/moderation, or encryption beyond future HTTPS transport. Local H2 is verified; PostgreSQL deployment verification remains release work.
