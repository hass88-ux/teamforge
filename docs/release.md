# Release build and launch checks

Build the frontend before packaging the Java API. The Maven `release` profile copies `frontend/dist` into the JAR's static resources. Java then serves the app and API from the same origin; a separate Vite server is unnecessary for this artifact. The recommendation service still runs separately.

Windows: run `./scripts/build-release.ps1` with Node 22, Java 21 and Maven on PATH. Stop the local TeamForge Java process before building on Windows because its JAR may be locked. The script installs locked frontend dependencies, checks lint/tests/build, and runs `mvn -B -f backend/pom.xml -Prelease clean verify`. Do not package without first building the frontend, or it can include missing/stale assets.

For a local release smoke test, use a separate port and database:

```powershell
java -jar backend/target/teamforge-api-0.1.0-SNAPSHOT.jar --server.port=8081 --spring.datasource.url="jdbc:h2:file:./.local/release-smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
```

Open `http://127.0.0.1:8081/`. Check that `/` returns HTML, the referenced `/assets/` JavaScript and CSS return 200, `/actuator/health` returns UP, and unauthenticated `/api/account/export` returns 401. Unknown routes and arbitrary files must stay denied. Test demo onboarding with the recommendation service running. The local launcher remains useful for Vite development at port 5173.

For production, activate `SPRING_PROFILES_ACTIVE=production`, set `DATABASE_URL` to a JDBC PostgreSQL URL, `DATABASE_USER`, `DATABASE_PASSWORD`, and `AI_SERVICE_URL` to the private recommendation endpoint. Use HTTPS at the public edge; Secure session cookies are enabled by the production profile. Keep the recommendation service private. Do not expose database credentials or local database files. Sessions currently live in one API process: use one replica until shared sessions are implemented.

Still required before public beta: provision hosting and PostgreSQL, verify all migrations/locking against PostgreSQL, exercise HTTPS cookies and same-origin routing, configure backups and restore checks, and establish report review and public privacy/contact information. This document and packaged artifact are preparation, not evidence of a deployed service.

Local verification on October 5: 48 Java tests passed; packaged HTML, JavaScript and CSS returned 200, health returned UP, and unauthenticated export returned 401 on port 8081 with an isolated H2 database. The existing development services also returned healthy responses.

## Repeatable smoke check

Run `./scripts/check-app.ps1` for the default local development services. Run `./scripts/check-app.ps1 -AppUrl http://127.0.0.1:8080 -Release` for the integrated JAR serving the frontend on the API port. Override AppUrl, ApiUrl and RecommendationUrl for another explicitly configured environment. This check sends only anonymous GET requests; it does not sign in or mutate data. Nonzero exit means a service, asset, or route protection failed. Windows PowerShell and PowerShell 7 response bodies are decoded consistently.
