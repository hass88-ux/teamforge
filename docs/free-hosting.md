# Free public preview setup

Researched October 5, 2026. Proposed setup: one Render Free Docker web service for bundled frontend, Java API and loopback-only Python recommendations; a Neon Free PostgreSQL project for persistent records. No hosted resources have been created yet.

Render's [free service documentation](https://render.com/docs/free) describes 750 shared instance hours per workspace/month, sleep after 15 minutes without inbound traffic, cold starts, and ephemeral local storage. Without a payment method, bandwidth/build overages suspend services/builds instead of billing. Do not add a card, upgrade a plan, buy a domain or enable paid features. If signup requires a card or payment commitment, stop and use another option. Render's free PostgreSQL expires after 30 days, so it is not the intended database.

Neon's [free plan information](https://neon.com/blog/neon-free-plan-1-gb-per-project) describes 1 GB/project storage at the time of research. Its [free-tier announcement](https://neon.com/blog/making-pricing-more-predictable) states signup without a credit card. Confirm current compute, egress, storage and inactivity rules in the account dashboard before creating a resource; plan details can change. Free previews are quota-limited, not an unlimited uptime guarantee.

Repository preparation:

- Dockerfile builds frontend and Java, then runs Java and Python in one non-root container. Python binds only to 127.0.0.1; the Java API alone is public.
- render.yaml explicitly selects `plan: free` and requests database credentials as secrets. It creates no Render database or paid disk.
- Production database pool is capped at three connections with no idle minimum. Sessions remain in memory and require signing in again after sleep/restarts.
- CI builds and smoke-tests the combined container at a 512 MiB memory limit. This is a preview-size check, not load testing or proof of provider uptime.

After the user completes account signup and provider terms:

1. Create a Neon Free project with a fresh database for beta accounts. Save credentials only in Render's secret environment fields; never commit or paste them into documentation.
2. Use a JDBC URL shaped like `jdbc:postgresql://HOST/DATABASE?sslmode=require`. Set DATABASE_USER and DATABASE_PASSWORD separately. Use the connection endpoint supplied by Neon.
3. Create a Render Docker web service from this repository, branch main, Dockerfile at repository root, Free plan. If Blueprint creation requests billing information, use manual free-service setup or stop; do not enter payment details.
4. Set SPRING_PROFILES_ACTIVE=production, the three database secrets and AI_SERVICE_URL=http://127.0.0.1:8001. Health path is /actuator/health/readiness. PORT is provided by Render.
5. Verify HTTPS, saved profile persistence, mutual matches, messaging, tasks and secure sessions on the actual deployed URL. Run the acceptance checklist. No real-user beta is claimed until these checks pass.

The configured application uses no paid language-model API. PostgreSQL data remains outside Render's ephemeral filesystem. Free accounts and terms need the user's participation; credentials and OAuth consent must stay under their control.

Verified container checkpoint: all jobs passed for f781eab in [GitHub Actions](https://github.com/hass88-ux/teamforge/actions/runs/37273953764), including Docker build and the 512 MiB combined-service smoke check. Actual Neon connectivity and Render HTTPS remain pending deployment. The user already has Render; Neon signup is outstanding.

October 7: the user approved credential transfer to Render and public deployment. Neon project/database teamforge is on Free/PostgreSQL 16. Render service teamforge is Free, and https://teamforge-jq44.onrender.com reached Live after eight successful Flyway migrations. Credentials were entered only in Render environment fields; none are in this repository. Hosted preview validation and production operations are separate from this startup result.
