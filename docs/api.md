# Onboarding API

`POST /api/onboarding/validate` validates a demo profile draft. It creates neither an account nor persisted state. The response explicitly identifies the draft as DEMO and `persisted: false`.

Required signals: displayName, role, skills, interests, rolesSought, weeklyHours (1–60), goal, workingStyle, IANA timezone, and availability.

Availability values are local weekly hour indexes: Monday 00:00 is 0, Tuesday 00:00 is 24, Sunday 23:00 is 167. Recommendation features must convert these using the supplied timezone before comparing schedules; daylight-saving transitions need explicit handling in the ranking service.

Collection and string bounds prevent unbounded profile payloads. Invalid input returns HTTP 400 without stack traces. This endpoint is an initial contract; profile ownership and persistence will be introduced with authenticated accounts.

## Demo recommendations

POST /api/demo/recommendations accepts the same profile draft and returns ranked fictional profiles, structured contributions, directional scores, reciprocal compatibility, evidence, and model version. Invalid profiles return 400; unreachable or failing AI service returns 503. Configure AI_SERVICE_URL on Java; the default is http://127.0.0.1:8001. No persisted interactions or real-user recommendations are created.

## Team recommendation

`POST /api/demo/teams` accepts `{ profile, project }`. Profile uses the onboarding contract. Project requires name (up to 80 characters), description (up to 1000), domain, stage, technologies (1–20), unique rolesNeeded (1–4), and weeklyHours (1–60). Invalid nested input returns 400; service failures return 503. Python returns feasible/members and, for a feasible team, teamScore, contributions, sharedHours, rolesCovered, and algorithm metadata. Recommendations neither persist projects nor create membership.
