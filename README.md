# TeamForge

Find the people you should build with.

TeamForge is being built as a collaborator matchmaking platform with structured, reciprocal recommendations. This repository is in the foundation stage. There is no public deployment or real-user traction yet.

## Current implementation

- React + TypeScript frontend scaffold (Vite).
- Java 21 Spring Boot API with health probes and a startup/health integration test.
- Architecture and ten-day delivery plan.

## Local development

Backend: install Java 21 and Maven, then run `mvn -f backend/pom.xml spring-boot:run`.
Health: `http://localhost:8080/actuator/health`.
Backend checks: `mvn -f backend/pom.xml verify`.
Frontend: run `npm ci` and `npm run dev` inside `frontend`.
Frontend checks: run `npm run build` inside `frontend`.

## Planned product

Interactive onboarding, isolated demo sessions, 250+ clearly fictional profiles, explained reciprocal ranking, discovery, mutual matches, projects, messaging, coffee-chat proposals, and team optimization. Real-user authentication and optional public-only GitHub enrichment are planned. These features are not yet implemented.

See [architecture](docs/architecture.md) and [delivery plan](docs/delivery-plan.md).

## Verification

Foundation verified locally: backend Maven verify (including health integration test), frontend lint, and frontend production build. CI repeats these checks. The landing page is an early product preview, not the completed demo.

