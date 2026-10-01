from datetime import datetime, timezone
from itertools import combinations
from .ranking import score, utc_slots


def recommend_team(owner, project, candidates, reference=None, beam_width=16, pool_per_role=8):
    """Bounded beam search; one distinct fictional candidate per requested role."""
    reference = reference or datetime.now(timezone.utc)
    roles = list(dict.fromkeys(project['rolesNeeded']))
    eligible = {p['id']: p for p in candidates if p.get('accountType') == 'DEMO' and p['id'] != owner.get('id') and p['weeklyHours'] >= project['weeklyHours']}
    pair_cache = {}
    slot_cache = {}
    def slots(p):
        key = p.get('id', 'visitor')
        if key not in slot_cache: slot_cache[key] = utc_slots(p, reference)
        return slot_cache[key]
    def pair(a, b):
        key = tuple(sorted((a.get('id', 'visitor'), b.get('id', 'visitor'))))
        if key not in pair_cache: pair_cache[key] = score(a, b, reference)['compatibility']
        return pair_cache[key]
    def evaluate(team):
        all_members = [owner] + team
        common = set(slots(owner))
        for member in team: common &= slots(member)
        pairs = [pair(a, b) for a, b in combinations(all_members, 2)]
        covered = len({p['role'] for p in team} & set(roles)) / max(1, len(roles))
        skill_set = {skill for member in all_members for skill in member['skills']}
        required = set(project['technologies'])
        skill_coverage = len(required & skill_set) / len(required) if required else 1
        domain_alignment = sum(project['domain'] in p['interests'] for p in all_members) / len(all_members)
        contributions = {'roleCoverage': 30 * covered, 'pairwiseCompatibility': 30 * (sum(pairs) / max(1, len(pairs))) / 100,
                         'skillCoverage': 15 * skill_coverage, 'domainAlignment': 15 * domain_alignment,
                         'sharedAvailability': 10 * min(len(common) / 3, 1)}
        return sum(contributions.values()), contributions, len(common)
    pools = {}
    for role in roles:
        candidates_for_role = [p for p in eligible.values() if p['role'] == role]
        pools[role] = sorted(candidates_for_role, key=lambda p: (-(pair(owner, p) + 15 * (project['domain'] in p['interests'])), p['id']))[:pool_per_role]
    missing = [role for role in roles if not pools[role]]
    if owner['weeklyHours'] < project['weeklyHours']:
        return {'feasible': False, 'members': [], 'reason': 'Your weekly commitment is below the project requirement.', 'missingRoles': missing}
    if missing:
        return {'feasible': False, 'members': [], 'reason': 'No eligible demo candidates can cover every requested role at this commitment.', 'missingRoles': missing}
    beam = [[]]
    for role in roles:
        expanded = [team + [candidate] for team in beam for candidate in pools[role] if candidate['id'] not in {p['id'] for p in team}]
        beam = sorted(expanded, key=lambda team: (-evaluate(team)[0], tuple(p['id'] for p in team)))[:beam_width]
    if not beam: return {'feasible': False, 'members': [], 'reason': 'No team satisfies the requested constraints.', 'missingRoles': roles}
    best = beam[0]
    total, contributions, common = evaluate(best)
    return {'feasible': True, 'members': best, 'teamScore': round(total), 'contributions': {k: round(v, 2) for k, v in contributions.items()},
            'sharedHours': common, 'rolesCovered': [p['role'] for p in best], 'includesProjectOwner': True,
            'algorithm': 'bounded-beam-v1', 'beamWidth': beam_width, 'candidatesPerRole': pool_per_role,
            'referenceDate': reference.date().isoformat(), 'accountType': 'DEMO', 'synthetic': True,
            'limitations': 'Approximate search over a pruned synthetic pool. This recommendation does not form a team or send invitations.'}
