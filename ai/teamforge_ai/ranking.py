from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

WEIGHTS = {'roles': 25, 'skills': 20, 'interests': 20, 'availability': 15, 'commitment': 10, 'goal': 5, 'style': 5}
MODEL_VERSION = 'weighted-reciprocal-v1'


def fraction(a, b):
    return len(set(a) & set(b)) / max(1, len(set(a)))


def utc_slots(profile, reference):
    """Compare the upcoming Monday-based week; recompute for DST changes."""
    local = reference.astimezone(ZoneInfo(profile['timezone']))
    monday = local.date() - timedelta(days=local.weekday())
    result = set()
    for slot in set(profile['availability']):
        local_hour = datetime.combine(monday + timedelta(days=slot // 24), datetime.min.time(), ZoneInfo(profile['timezone'])) + timedelta(hours=slot % 24)
        utc = local_hour.astimezone(timezone.utc)
        # Skip nonexistent local wall hours during spring-forward.
        if utc.astimezone(ZoneInfo(profile['timezone'])).replace(tzinfo=None) == local_hour.replace(tzinfo=None):
            result.add(utc)
    return result


def directional(a, b, reference):
    a_slots, b_slots = utc_slots(a, reference), utc_slots(b, reference)
    overlap = len(a_slots & b_slots)
    features = {
        'roles': float(b['role'] in a['rolesSought']),
        'skills': len(set(b['skills']) - set(a['skills'])) / max(1, len(set(b['skills']))),
        'interests': fraction(a['interests'], b['interests']),
        'availability': overlap / max(1, min(len(a_slots), len(b_slots))),
        'commitment': min(a['weeklyHours'], b['weeklyHours']) / max(a['weeklyHours'], b['weeklyHours']),
        'goal': float(a['goal'] == b['goal']),
        'style': 1.0 if a['workingStyle'] == b['workingStyle'] else 0.5 if 'Flexible' in (a['workingStyle'], b['workingStyle']) else 0.0,
    }
    contributions = {key: round(value * WEIGHTS[key], 2) for key, value in features.items()}
    return sum(contributions.values()), contributions, overlap


def score(a, b, reference=None):
    reference = reference or datetime.now(timezone.utc)
    forward, contributions, overlap = directional(a, b, reference)
    reverse, reverse_contributions, _ = directional(b, a, reference)
    # Harmonic mean penalizes one-sided compatibility; these are scores, not calibrated probabilities.
    reciprocal = 2 * forward * reverse / (forward + reverse) if forward + reverse else 0
    shared = sorted(set(a['interests']) & set(b['interests']))
    new_skills = sorted(set(b['skills']) - set(a['skills']))
    evidence = []
    if b['role'] in a['rolesSought']: evidence.append(f"They bring a role you are seeking: {b['role']}.")
    if a['role'] in b['rolesSought']: evidence.append(f"They are looking for your role: {a['role']}.")
    if new_skills: evidence.append('They add skills: ' + ', '.join(new_skills[:4]) + '.')
    if shared: evidence.append('Shared project interests: ' + ', '.join(shared) + '.')
    evidence.append(f'{overlap} selected hour(s) overlap in the reference week after timezone conversion.')
    if a['goal'] == b['goal']: evidence.append('You share the same collaboration goal: ' + a['goal'] + '.')
    return {'compatibility': round(reciprocal), 'forwardScore': round(forward, 2), 'reverseScore': round(reverse, 2), 'contributions': contributions, 'reverseContributions': reverse_contributions, 'evidence': evidence, 'overlapHours': overlap, 'modelVersion': MODEL_VERSION, 'referenceWeek': reference.date().isoformat()}


def rank(profile, candidates, reference=None):
    eligible = [candidate for candidate in candidates if candidate.get('accountType') == 'DEMO' and candidate['id'] != profile.get('id')]
    return sorted([{'candidate': candidate, **score(profile, candidate, reference)} for candidate in eligible], key=lambda item: (-item['compatibility'], item['candidate']['id']))
