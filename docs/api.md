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
