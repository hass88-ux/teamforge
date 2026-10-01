# Recommendation baseline

Python owns the recommendation logic. Spring validates the incoming profile and calls the private FastAPI service with bounded connect/read timeouts. The frontend calls Java through the same-origin API path.

## Model 0: demo eligibility

Candidates must be labeled DEMO and cannot be the querying profile. Real-user ranking now accepts only opted-in stored candidates through a separate REAL endpoint. There are 600 deterministic fictional profiles across 12 role archetypes, varied domains, project goals, experience levels, timezones, schedules, working styles, and commitment levels. No seeded record represents a real person.

## Model 1: weighted reciprocal compatibility

Directional contributions (maximum 100): desired role 25, offered/needed or complementary skills 20, shared interests 20, schedule overlap 15, commitment ratio 10, shared goal 5, working style 5. Roles use an explicit preference; complementarity measures candidate skills absent from the querying profile; interests measure coverage of the querying profile's interests. Working style is exact-match or partial credit for flexibility.

Availability is converted from local weekly hours to UTC instants for a reference week. Nonexistent spring-forward wall hours are skipped; ambiguous fall-back hours use the first occurrence. Weekly schedules are approximate and do not constitute calendar bookings. Fractional-offset timezones currently require identical hour start instants; partial-hour overlap is a known limitation.

The displayed reciprocal score is the harmonic mean of both directional scores. This penalizes one-sided compatibility and is symmetric. It is a provisional design choice, not an experimentally proven winner against minimum, geometric mean, or learned reciprocal scoring. Scores are not calibrated probabilities of likes or successful collaboration. UI explanations are generated directly from structured features, with no language-model inference.

## Demo cold start

If the initial top score is below 85, create a clearly disclosed fictional candidate with the sought role and complementary skills, shared interests, availability, commitment, goal, and style. Rank this candidate with the ordinary function. The UI labels the candidate as tailored for the demo. No real-user matching guarantee exists.

## Verification and limitations

Nine ranking tests verify dataset identity/diversity, demo guarantee across all role/intent combinations, scoring symmetry/bounds/contribution sums, timezone conversion, self/real-profile exclusion, and stronger ranking for an explicitly compatible pair. These are correctness tests, not evidence of real-world predictive quality. No ML evaluation, user traction, or synthetic ranking benchmark has been claimed.

The browser walkthrough verified onboarding → Java validation → Python recommendations → scores/evidence → fictional projects → like → pass. Demo choices are temporary browser state; simulated matches and scripted demo messaging are now implemented; real-user mutual matches and interaction persistence remain future work. Candidate ranking is an exhaustive CPU baseline suitable for this small demo pool; larger datasets will require retrieval and caching.

## Intent and skill demand (v2)

Eligibility excludes provider/provider and seeker/seeker pairs before ranking. Collaborators remain eligible for any intent; individuals and organizations follow the same rules. With explicit needed skills, the skill component averages directional offered/needed coverage. This rewards shared Java when a provider offers Java and a seeker needs Java/Python. Without explicit demand the existing complementary skill signal applies. Demo discovery filters displayed reciprocal scores strictly above 50%; a tailored fictional candidate has a compatible intent and fulfills selected needs. Scores are heuristics, not success probabilities. Descriptions remain editable context rather than machine-interpreted evidence. Team search also enforces intent eligibility against the owner and every member.

Real discovery shares the weighted scoring function but filters on REAL account type and never constructs a demo candidate. Java retrieves up to 200 recently updated visible profiles; this is a bounded preview retrieval strategy, not comprehensive search across every account.
