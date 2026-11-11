# Vercel frontend deployment

Use the existing GitHub repository, branch main, Root Directory `frontend`, framework Vite, `npm ci`, build `npm run build`, output `dist`. Configuration lives in frontend/vercel.json. No database secrets belong in frontend environment variables.

The frontend is static on Vercel. Requests under /api are proxied to the existing Render Java service; Python stays private on Render and account records remain in Neon. Cookies are HttpOnly, Secure and host-only, so a new frontend domain requires signing in once. API responses must never be CDN-cached. Do not disable CSRF or expose the database to browsers.

Vercel Hobby is limited to personal non-commercial use. Do not enable a paid trial or plan without owner instruction. Render Free cold starts still affect API requests; Vercel makes the public page independent of that startup. Validate the complete acceptance script against the new domain before promoting it as the primary URL. Keep the Render URL available for rollback.

November 11 diagnostic: frontend build/lint and 13 tests pass; 57 Java tests pass. Render initially timed out, then health returned UP and the homepage returned HTTP 200. Anonymous account export returned 401. Fixed workspace session-expiry handling and added search/social descriptions. Search-engine indexing is not guaranteed by hosting; confirm the final public domain before adding canonical URLs, sitemap and Search Console verification. Brevo, moderator activation, manual backup/restore operations and real-user ranking evaluation remain outstanding.

Production frontend: https://teamforge-teal.vercel.app (Vercel Hobby, GitHub main, root frontend). The homepage and /api/auth/csrf returned 200; anonymous account export returned 401 with private API caching disabled. The frontend intentionally does not proxy Actuator; run `python scripts/acceptance-smoke.py https://teamforge-teal.vercel.app https://teamforge-jq44.onrender.com` to check backend health separately while exercising all account workflows through Vercel.

The full two-account acceptance check passed through the Vercel domain, including session cookies/CSRF, mutual matching, messaging, coffee, projects/tasks, safety/report access, export, blocking, recovery and session revocation. Disposable accounts and related records were removed.
