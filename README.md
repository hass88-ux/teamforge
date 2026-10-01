# TeamForge

Find the people you should build with.

TeamForge is being built as a collaborator matchmaking platform with structured, reciprocal recommendations. This repository has a working local demo and is still in development. There is no public deployment or real-user traction yet.

## Current implementation

- React + TypeScript landing page and six-step interactive demo onboarding (Vite).
- Java 21 Spring Boot API with health probes and a startup/health integration test.
- Python/FastAPI reciprocal weighted ranking with 264 diverse fictional profiles, grounded explanations, and disclosed tailored demo candidates.
- Discovery cards, synthetic project details, and temporary like/pass controls.
- Disclosed simulated mutual matches, scripted demo conversations, and coffee-chat proposals with simulated accept/decline controls.
- Temporary project creation and constrained team recommendations with inspectable scoring.
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

Interactive onboarding, isolated demo sessions, 250+ clearly fictional profiles, explained reciprocal ranking, discovery, mutual matches, projects, messaging, coffee-chat proposals, and team optimization. Real-user authentication and optional public-only GitHub enrichment are planned. Demo onboarding and baseline discovery are implemented. Real-user mutual matching and messaging, persistence, accounts, trained models, and GitHub OAuth remain unimplemented. Demo team recommendations use a bounded beam-search heuristic; real-user team formation is not yet implemented. Demo matches and conversations are clearly labeled simulations.

See [architecture](docs/architecture.md), [demo system](docs/demo-system.md), [recommendation baseline](docs/recommendation-system.md), [team formation](docs/team-formation.md), and [delivery plan](docs/delivery-plan.md).

## Verification

Current local checks pass: 8 Java tests, 11 Python tests, 6 frontend state tests, frontend lint, and production build. CI runs these suites. Browser walkthroughs cover onboarding, scored discovery, fictional projects, simulated match/message/coffee chat, project creation, feasible team coverage, and an honest infeasible result.

No public deployment, real-user metrics, learned-model evaluation, or production security certification is claimed. Demo state is temporary and fictional profiles and automated replies are disclosed throughout.
