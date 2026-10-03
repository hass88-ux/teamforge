# Privacy decisions

Real accounts collect email and a password hash for authentication, plus the user-selected collaboration profile. Never expose email or hashes through recommendation candidates. Full stored profiles are accessible only to their owner. Profiles start private; an explicit discovery opt-in shares only public collaboration fields with signed-in members. Email, password hashes, timezone, and selected availability slots are not returned in discovery candidates. Users can withdraw visibility anytime. Passwords are not persisted in plaintext, and API validation does not echo them.

The optional GitHub connection is not implemented yet. No private GitHub repository data or OAuth tokens are collected. Demo profiles, projects, matches, and scripted replies use demo labels and identify simulated replies. Demo profile drafts and collaboration state are temporary and isolated from persistent accounts.

Owner-only export, password-confirmed deletion, and private recovery-key reset are implemented. Email verification, email recovery, retention policies, and a public privacy notice remain release work. The current local application is not a launched public account service. Fictional QA accounts used for local verification do not count as people or traction.

Real decisions and conversations now persist. Only the two active matched members can access messages; existing matches remain when either user hides discovery visibility. Unmatching revokes access for both people and prevents further contact in this preview. Closed match/message records remain stored. No email notification or scripted reply is sent in real conversations. A public privacy notice, retention policy, are still release work.
