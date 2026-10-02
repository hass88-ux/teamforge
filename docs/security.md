# Authentication and security

Implemented: password registration/login, BCrypt cost 12 hashes, cookie-backed Spring Security sessions, login session-ID rotation, logout invalidation, and CSRF tokens for account/profile writes. Passwords require 12–72 characters and at most 72 UTF-8 bytes to prevent BCrypt truncation. No credentials or session tokens are stored in frontend localStorage.

`GET /api/auth/csrf` provides the token/header name for same-origin requests. Profile reads/writes require authentication; ownership comes only from the session identity. There is no arbitrary account-ID profile endpoint. Request validation errors return sanitized messages without echoing password values. Public demo calculation endpoints are exempt from CSRF because they do not mutate account or persistent data.

Session cookies are HttpOnly and SameSite=Lax; production configuration requires Secure cookies and HTTPS. Sessions expire after 30 minutes of inactivity and are currently held in server memory, so restarting invalidates login sessions. Accounts and profiles survive independently in the database.

Signup/login have a basic per-instance remote-address limit of 30 attempts per five minutes and a bounded address map. This is not a distributed abuse-prevention system. No proxy headers are trusted for identifying clients; deployment behind a proxy needs an explicitly reviewed rate-limit strategy.

Default backend binding is loopback. There is no public deployment yet. project persistence, email verification, password recovery, account deletion, distributed sessions, and broader abuse controls are unfinished. These limitations must be resolved or clearly constrained before a public real-user beta. Current functionality is not a security certification.

## Verified

Seven auth/profile integration tests cover CSRF, registration/hashing, login rotation/logout, incorrect credentials, profile ownership/isolation, sanitized validation errors, byte-length rejection, and no persistent account/profile writes from demo validation. The existing backend regression tests still pass. A local browser walkthrough created a fictional QA account and saved a profile; backend restart and subsequent login recovered the same stored profile, and logout returned to the landing page. Live demo recommendations still worked without an account. No QA account constitutes real-user traction.

## Discovery boundary

Authenticated discovery retrieves only explicitly visible profiles and excludes the session owner. A bounded real-only ranking request goes to the loopback/private recommendation service, with no credentials. The Java response reconstructs candidates from the eligible database snapshot and an allowlist of public fields, excluding email, hashes, timezone, and availability slots. Visibility is rechecked after inference. Visibility writes require authenticated ownership and CSRF, and do not accept another account ID. Four integration tests cover private defaults, visibility reversal/edit persistence, self/private exclusion and field minimization, honest empty results, and retryable upstream failures. Three Python tests verify real/demo isolation, intent and threshold rules, self exclusion, and bounded input pools.

## Mutual consent and message authorization

Flyway V3 stores real decisions, canonical unordered account pairs, and messages. Reciprocal likes serialize locks in canonical account-ID order so concurrent votes create one match. Likes re-score the pair using the real ranking endpoint before the transaction; locked profile versions must still match the scored snapshot. Invalid/stale/unavailable pairs fail without persisting choices. Discovery omits decided candidates after refresh.

Each conversation read/send checks active membership using the session account, never a client-provided sender. Reads and writes lock the match against concurrent closure. Send requests are idempotent per match/sender/client UUID. Sender-account locks serialize a database-backed 30-new-messages/minute limit across that user's conversations. Unmatching closes access and blocks future contact for that pair. Hiding discovery does not terminate existing consent; users can explicitly unmatch. Closed records are retained, not deleted.

Eight collaboration tests cover mutual consent and persistence, intent/visibility/self/score rules, outsider access and CSRF, replay-safe sending, closure, simultaneous likes, validation/rate limits, pagination, and profile changes during scoring. Discovery adds a regression for persistent decision exclusion. These checks do not replace production abuse controls, reporting, or PostgreSQL verification.

Coffee invitations use the same locked active-membership checks as messages. The server derives the proposer; only the recipient accepts or declines. Matched members may cancel upcoming invitations. Creation is retry-safe and bounded to ten future pending invitations per match. Three integration tests cover membership/CSRF/time validation, duplicate retries/response roles/final-state protection, and pending limits/closed-match access.

Projects are private to owners and invited/accepted members. Only an owner can invite, and the target is resolved from an active server-authorized mutual match. Invitees must consent before becoming team members. Declining removes project access; accepted membership is independent of match state. Project roster shares display names and member IDs, never email or private availability. All writes require CSRF. Creation and invitations have serialized preview caps. Leave/removal controls must be added before public beta.
