# Interactive demo onboarding

The landing page's Try demo button opens six focused steps: identity/role, skills, domains/goals, teammate roles, commitment/style, and weekly availability.

Answers exist only in React memory. Exit and reload clear them; no shared demo account or real-user record is created. Finish submits to the Spring onboarding validation API through Vite's development proxy. A failed request preserves answers and allows retry. The completion view explicitly says DEMO PROFILE / NOT SAVED.

Controls support keyboard activation, selected-state announcements, labeled inputs, progress, and focus on each new step. The weekly selector currently exposes six common hours each day. Arbitrary hour selection and full recommendation discovery are future work.

## Verification

Local browser walkthrough completed all six steps, selected two weekly slots, submitted to Spring, and displayed the expected profile summary. Frontend lint and production build passed. Backend validation tests were already passing; this milestone does not change the backend.

## Production routing

Vite's proxy is development-only. Public hosting must route `/api` to the Java backend through a same-origin reverse proxy. Do not deploy the static frontend alone and claim the demo works.

## Discovery milestone

Completed profiles can now open ranked recommendations, view contribution breakdowns and fictional projects, and like/pass candidates. Demo actions are isolated in React memory. See recommendation-system.md for synthetic-candidate disclosure and scoring limitations.
