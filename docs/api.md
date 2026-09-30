# Onboarding API

`POST /api/onboarding/validate` validates a demo profile draft. It creates neither an account nor persisted state. The response explicitly identifies the draft as DEMO and `persisted: false`.

Required signals: displayName, role, skills, interests, rolesSought, weeklyHours (1–60), goal, workingStyle, IANA timezone, and availability.

Availability values are local weekly hour indexes: Monday 00:00 is 0, Tuesday 00:00 is 24, Sunday 23:00 is 167. Recommendation features must convert these using the supplied timezone before comparing schedules; daylight-saving transitions need explicit handling in the ranking service.

Collection and string bounds prevent unbounded profile payloads. Invalid input returns HTTP 400 without stack traces. This endpoint is an initial contract; profile ownership and persistence will be introduced with authenticated accounts.
