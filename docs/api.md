# Onboarding API

`POST /api/onboarding/validate` validates a demo profile draft. It creates neither an account nor persisted state. The response explicitly identifies the draft as DEMO and `persisted: false`.

Required signals: displayName, role, skills, interests, rolesSought, weeklyHours (1–60), goal, workingStyle, IANA timezone, and availability.

Availability values are local weekly hour indexes: Monday 00:00 is 0, Tuesday 00:00 is 24, Sunday 23:00 is 167. Recommendation features must convert these using the supplied timezone before comparing schedules; daylight-saving transitions need explicit handling in the ranking service.

Collection and string bounds prevent unbounded profile payloads. Invalid input returns HTTP 400 without stack traces. This endpoint is an initial contract; authenticated profile ownership and persistence use the endpoints below.

## Demo recommendations

POST /api/demo/recommendations accepts the same profile draft and returns ranked fictional profiles, structured contributions, directional scores, reciprocal compatibility, evidence, and model version. Invalid profiles return 400; unreachable or failing AI service returns 503. Configure AI_SERVICE_URL on Java; the default is http://127.0.0.1:8001. No persisted interactions or real-user recommendations are created.

## Team recommendation

`POST /api/demo/teams` accepts `{ profile, project }`. Profile uses the onboarding contract. Project requires name (up to 80 characters), description (up to 1000), domain, stage, technologies (1–20), unique rolesNeeded (1–4), and weeklyHours (1–60). Invalid nested input returns 400; service failures return 503. Python returns feasible/members and, for a feasible team, teamScore, contributions, sharedHours, rolesCovered, and algorithm metadata. Recommendations neither persist projects nor create membership.

## Accounts and profiles

- GET `/api/auth/csrf`: obtain the CSRF token and header name; treat both session cookies and tokens as credentials.
- POST `/api/auth/signup`: email/password registration, login session creation, 201. Password: 12–72 characters, at most 72 UTF-8 bytes. Duplicate details return 409.
- POST `/api/auth/login`: email/password session login, 200; incorrect credentials return 401.
- GET `/api/auth/me`: the caller's account UUID, email, and REAL account type; anonymous requests return 401.
- POST `/api/auth/logout`: CSRF-protected session invalidation, 204.
- GET/PUT `/api/profiles/me`: retrieve/update the caller's validated structured profile; never accepts an owner ID. Missing profile returns 404. Save response includes REAL / persisted=true.

CSRF is required on signup, login, logout, and profile writes. Invalid fields return sanitized 400 responses. Basic authentication rate limits return 429. Account/profile responses never expose password hashes; CSRF tokens are returned only by the explicit CSRF endpoint.

## Real discovery

- PUT `/api/profiles/me/visibility`: `{ "discoverable": true | false }`, authenticated + CSRF; changes only the caller's existing profile. New and migrated profiles default to false. Returns the current profile view with `discoverable`.
- GET `/api/discovery/recommendations`: authenticated, server derives the query profile from the session. Returns REAL, `recommendations`, `searchedProfileCount`, and `poolLimited`. Requires a saved profile (409 otherwise); upstream failures return 503; an empty visible pool returns an empty list without calling Python. Candidate output contains allowlisted public fields, not email, credentials, timezone, or schedule slots. No interaction is recorded.
- Private Python POST `/recommendations/real`: validated owner profile and up to 200 REAL candidates; ranks the provided pool without seeded/demo fallback. The service must stay private in deployment.

## Real interactions

- POST `/api/discovery/decisions`: `{ candidateId: UUID, decision: "LIKE" | "PASS" }`, authenticated + CSRF. Owner is derived from the session. Self decisions return 400; unavailable profiles return 404; incompatible intent, hidden liker, stale profile, closed pair, or compatibility at/below 50% return 409. Ranking failures return 503 and persist no choice. Response includes `matched` and an opaque `matchId` for an existing/new mutual match. Decisions survive refresh and are excluded from new discovery batches.
- GET `/api/matches`: up to 100 newest active matches belonging to the caller, with the other member's name, type, intent, score at matching, and creation time. No email, credentials, or private schedule fields.
- GET `/api/matches/{id}/messages?after=0`: active members only; up to 50 messages ordered by sequence, plus `hasMore` and `nextAfter`. Missing, closed, or unauthorized conversations all return 404.
- POST `/api/matches/{id}/messages`: `{ clientId: UUID, text: string }`, authenticated + CSRF and active membership; nonblank, at most 1000 characters. Same user/match/client ID repeats return the original message; a changed body with that ID returns 409. At most 30 new messages per sender per minute (429 thereafter). The server assigns sender, sequence, and timestamp.
- DELETE `/api/matches/{id}`: authenticated + CSRF and active membership, 204. Closes the pair, removes it from active lists, and prevents all further conversation access/contact. Stored records remain; retention/deletion controls are pending.

## Matched coffee invitations

GET/POST `/api/matches/{id}/proposals`: active members only. Creation requires CSRF plus `{clientId, kind, startsAt, timezone, note}`. Kind is Virtual coffee, Intro call, or Project discussion; startsAt is a future UTC ISO instant within 180 days; timezone must be a valid IANA zone; note is at most 500 characters. Each invitation represents a 30-minute conversation. Matching client IDs are retry-safe; changed details return 409. Up to ten future pending invitations per match; excess returns 429. GET returns at most 100 recent invitations.

POST `/api/matches/{id}/proposals/{proposalId}/response` with `{status: ACCEPTED|DECLINED|CANCELLED}` requires CSRF and membership. Only the recipient accepts/declines a pending invitation. Either member can cancel an upcoming pending/accepted invitation. Final/conflicting or elapsed-time transitions return 409; identical repeated responses return the saved state. Closed/unauthorized matches return 404. No external notification, calendar event, or meeting URL is generated.
