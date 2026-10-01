# Interactive demo onboarding

The landing page's Try demo button opens six focused steps: identity/role, skills, domains/goals, teammate roles, commitment/style, and weekly availability.

Answers exist only in React memory. Exit and reload clear them; no shared demo account or real-user record is created. Finish submits to the Spring onboarding validation API through Vite's development proxy. A failed request preserves answers and allows retry. The completion view explicitly says DEMO PROFILE / NOT SAVED.

Controls support keyboard activation, selected-state announcements, labeled inputs, progress, and focus on each new step. The weekly selector currently exposes six common hours each day. Arbitrary hour selection is future work; baseline recommendation discovery is implemented.

## Verification

Local browser walkthrough completed all six steps, selected two weekly slots, submitted to Spring, and displayed the expected profile summary. Frontend lint and production build passed. Backend validation tests were already passing; this milestone does not change the backend.

## Production routing

Vite's proxy is development-only. Public hosting must route `/api` to the Java backend through a same-origin reverse proxy. Do not deploy the static frontend alone and claim the demo works.

## Discovery milestone

Completed profiles can now open ranked recommendations, view contribution breakdowns and fictional projects, and like/pass candidates. Demo actions are isolated in React memory. See recommendation-system.md for synthetic-candidate disclosure and scoring limitations.

## Simulated collaboration milestone

A visitor's like produces a simulated match only when the fictional candidate's mutual compatibility score is strictly above 50%. This is a disclosed demo rule, not a human decision or learned acceptance probability. Passes and scores of 50% or below do not produce simulated matches; repeated decisions cannot duplicate matches.

Only existing demo matches open the conversation UI. Messages are trimmed and limited to 1000 characters. A scripted reply is explicitly labeled DEMO / SIMULATED REPLY. Coffee-chat proposals support virtual coffee, intro call, or project discussion; dates must be future valid local times. Scheduling uses the browser's visibly identified timezone, stores a UTC instant and timezone, and rejects nonexistent local wall times. Users explicitly click simulate acceptance or decline. No real recipient, email, invitation, meeting link, or calendar integration exists.

All choices, messages, and proposals live in React memory; returning from a conversation to discovery preserves them. Leaving discovery for the profile, exiting, resetting the demo batch, or reloading clears them. This is not a production messaging authorization boundary: real messaging requires server-side identities, persisted mutual consent, and authorization before release.

Six frontend tests cover simulated reciprocity, duplicate decisions, unmatched-message rejection, message constraints, proposal timing and finalization, and reset. A local browser walkthrough verified match → message/scripted reply → future proposal → simulated acceptance. Frontend test, lint, and production build pass.

## Demo projects and teams

Completed profiles can open Create a project & team. Temporary project drafts request a constrained fictional team through Java and Python. The owner is included, one distinct demo candidate fills each requested role, and all people must meet the project commitment. Shared availability and project skill/domain coverage contribute to an inspectable heuristic score. No real invitations or membership are created. See team-formation.md for the bounded beam search and limitations.

The interface uses concise demo labels rather than repeated fictional/synthetic labels on profile cards, descriptions, and projects. Simulated replies and demo actions remain identified; the data is still synthetic and isolated from real accounts.
