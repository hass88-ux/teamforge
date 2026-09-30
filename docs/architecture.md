# Architecture decisions

React with TypeScript and Vite provides a static frontend suitable for inexpensive hosting. Spring Boot owns accounts, authorization, profiles, interactions, projects, conversations, and persistence. PostgreSQL will use versioned Flyway migrations; production schema changes must never use Hibernate auto-update.

A separate Python/FastAPI service will own feature engineering, reciprocal ranking, evaluation, and team optimization. Begin with eligibility filters and weighted compatibility. Compare feature-based learning against this baseline before adopting an advanced model. No paid language-model inference is needed for the core product.

Demo sessions must be isolated from real accounts. Every fictional candidate and simulated reply must be visible as demo content. Demo-compatible candidates are scored normally; real users receive no artificial mutual-match guarantee.

OAuth and database credentials belong in deployment environment variables, never Git. GitHub enrichment is optional and public-only. Production release requires authorization tests, migration checks, abuse controls, and a tested public demo journey.
