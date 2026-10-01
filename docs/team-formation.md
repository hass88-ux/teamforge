# Demo team formation

## Implemented flow

Completed demo profiles can create temporary project drafts with a name, problem description, domain, stage, technologies, up to four distinct additional roles, and weekly hours per person. The profile owner is always included. Drafts live in browser memory and clear when leaving the project screen or reloading.

Java validates the nested profile and project contract at `POST /api/demo/teams`, then calls Python `POST /teams/demo` with bounded timeouts. Python searches only the 600 seeded fictional profiles; it does not construct a guaranteed team or send invitations.

## Constraints and search

Hard constraints: one additional candidate for each requested distinct role, no repeated candidate IDs, DEMO account type, no candidate identical to the owner, and weekly commitment at least the project requirement for all selected people including the owner.

For each role, retain up to eight candidates ordered by owner compatibility with a domain-interest bonus. Expand a beam of up to sixteen partial teams through the requested roles. Score each combination and retain the best partial teams. Return the highest scoring complete retained team. This bounded beam search is an approximate optimization heuristic over a pruned pool, not proof of a global optimum.

No feasible result returns an explicit reason and missing roles when applicable. This never fabricates members. Maximum suggested team size is owner plus four collaborators. Domain alignment, skill coverage, and shared hours are soft objectives; the result may have uncovered skills or zero shared selected hours, and reports those outcomes.

## Team score

Maximum score 100:

- Additional role coverage: 30.
- Mean reciprocal pairwise compatibility across owner and candidates: 30.
- Coverage of requested project technologies/skills: 15.
- Fraction of people sharing the project domain interest: 15.
- Shared whole-team availability: 10 (saturates at three selected hours).

Explanations expose the actual objective contributions and shared hour count. This is a heuristic score, not a probability of project success. Pairwise scoring inherits the baseline's timezone and matching limitations. The algorithm caches pair scores and converted weekly slots per request and uses CPU only.

## Verified behavior

Five Python team tests verify distinct role coverage and contribution sums, exclusion of real/undercommitted candidates, owner commitment, explicit missing-role results, and preference for a more compatible team in a controlled pool. The Java bridge test verifies forwarding of nested team constraints and rejects duplicate requested roles. Browser verification covered project creation, a feasible frontend/design team, saved draft rendering, and an infeasible owner-commitment case.

No real team membership, project persistence, invitation acceptance, or real-user team recommendations exist yet. No performance superiority or benchmark results are claimed.
