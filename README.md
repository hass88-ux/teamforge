# TeamForge

Find the people you should build with.

TeamForge is being built as a collaborator matchmaking platform with structured, reciprocal recommendations. This repository is in the foundation stage. There is no public deployment or real-user traction yet.

## Current implementation

- React + TypeScript landing page and six-step interactive demo onboarding (Vite).
- Java 21 Spring Boot API with health probes and a startup/health integration test.
- Python/FastAPI reciprocal weighted ranking with 264 diverse fictional profiles, grounded explanations, and disclosed tailored demo candidates.
- Discovery cards, synthetic project details, and temporary like/pass controls.
- Architecture, CI, and ten-day delivery plan.

## Local development

Windows: after installing dependencies and building the backend, run `./scripts/start-local.ps1` from PowerShell to start all three services in hidden background processes. Open `http://127.0.0.1:5173/`. Logs are in `%TEMP%/teamforge-local`. This launcher uses the last built backend JAR; run Maven verify after backend changes before restarting it. Services remain local and do not create a public deployment.

Backend: install Java 21 and Maven, then run `mvn -f backend/pom.xml spring-boot:run`.
Health: `http://localhost:8080/actuator/health`.
Backend checks: `mvn -f backend/pom.xml verify`.
Frontend: run `npm ci` and `npm run dev` inside `frontend`.
Frontend checks: run `npm run lint` and `npm run build` inside `frontend`.
AI service: follow [ai/README.md](ai/README.md) and start it on port 8001 before discovery. Start Java and Vite alongside it. The frontend development proxy forwards /api to Java on port 8080. Production needs same-origin API routing.

## Planned product

Interactive onboarding, isolated demo sessions, 250+ clearly fictional profiles, explained reciprocal ranking, discovery, mutual matches, projects, messaging, coffee-chat proposals, and team optimization. Real-user authentication and optional public-only GitHub enrichment are planned. Demo onboarding and baseline discovery are implemented. Mutual matches, messaging, persistence, real accounts, team optimization, trained models, and GitHub OAuth remain unimplemented.

See [architecture](docs/architecture.md) and [delivery plan](docs/delivery-plan.md).

## Verification

Foundation verified locally: backend Maven verify (including health integration test), frontend lint, and frontend production build. CI repeats these checks. The landing page is an early product preview, not the completed demo.

- Validated demo onboarding draft API connected to the frontend; drafts are temporary and not persisted.
