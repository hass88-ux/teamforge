from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

WEIGHTS = {'roles': 25, 'skills': 20, 'interests': 20, 'availability': 15, 'commitment': 10, 'goal': 5, 'style': 5}
MODEL_VERSION = 'weighted-reciprocal-v2.1'


def intent_compatible(a, b):
    left, right = a.get('matchingIntent', 'COLLABORATOR'), b.get('matchingIntent', 'COLLABORATOR')
    return not (left == right and left in ('PROVIDER', 'SEEKER'))


def skill_fit(a, b):
    # Demand is directional: what I need from their offer, or what they need from mine.
    demand = a.get('neededSkills', [])
    reverse_demand = b.get('neededSkills', [])
    fits = []
    if demand: fits.append(fraction(demand, b['skills']))
    if reverse_demand: fits.append(fraction(reverse_demand, a['skills']))
    if fits: return sum(fits) / len(fits)
    return len(set(b['skills']) - set(a['skills'])) / max(1, len(set(b['skills'])))


def fraction(a, b):
    return len(set(a) & set(b)) / max(1, len(set(a)))


def utc_slots(profile, reference):
    """Use one UTC-selected calendar week for every profile, then convert wall hours.

    Selecting local weeks independently can put timezones seven days apart.
    Include neighbouring local weeks so recurring Sunday/Monday hours wrap
    into the same UTC window; timezone/DST conversion still uses actual dates.
    """
    anchor = reference.astimezone(timezone.utc)
    monday = anchor.date() - timedelta(days=anchor.weekday())
    window_start = datetime.combine(monday, datetime.min.time(), timezone.utc)
    window_end = window_start + timedelta(days=7)
    result = set()
    for slot in set(profile['availability']):
        for week_shift in (-7, 0, 7):
            local_hour = datetime.combine(monday + timedelta(days=slot // 24 + week_shift), datetime.min.time(), ZoneInfo(profile['timezone'])) + timedelta(hours=slot % 24)
            utc = local_hour.astimezone(timezone.utc)
            # Skip nonexistent local wall hours during spring-forward.
            if window_start <= utc < window_end and utc.astimezone(ZoneInfo(profile['timezone'])).replace(tzinfo=None) == local_hour.replace(tzinfo=None):
                result.add(utc)
    return result


def directional(a, b, reference):
    a_slots, b_slots = utc_slots(a, reference), utc_slots(b, reference)
    overlap = len(a_slots & b_slots)
    features = {
        'roles': float(b['role'] in a['rolesSought']),
        'skills': skill_fit(a, b),
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
    if not intent_compatible(a, b): reciprocal = 0
    shared = sorted(set(a['interests']) & set(b['interests']))
    new_skills = sorted(set(b['skills']) - set(a['skills']))
    evidence = []
    fulfilled = sorted((set(a.get('neededSkills', [])) & set(b['skills'])) | (set(b.get('neededSkills', [])) & set(a['skills'])))
    if fulfilled: evidence.append('Offered skills meet stated needs: ' + ', '.join(fulfilled) + '.')
    if b['role'] in a['rolesSought']: evidence.append(f"They bring a role you are seeking: {b['role']}.")
    if a['role'] in b['rolesSought']: evidence.append(f"They are looking for your role: {a['role']}.")
    if new_skills: evidence.append('They add skills: ' + ', '.join(new_skills[:4]) + '.')
    if shared: evidence.append('Shared project interests: ' + ', '.join(shared) + '.')
    evidence.append(f'{overlap} selected hour(s) overlap in the reference week after timezone conversion.')
    if a['goal'] == b['goal']: evidence.append('You share the same collaboration goal: ' + a['goal'] + '.')
    return {'compatibility': round(reciprocal), 'forwardScore': round(forward, 2), 'reverseScore': round(reverse, 2), 'contributions': contributions, 'reverseContributions': reverse_contributions, 'evidence': evidence, 'overlapHours': overlap, 'modelVersion': MODEL_VERSION, 'referenceWeek': reference.date().isoformat()}


def rank(profile, candidates, reference=None, account_type='DEMO'):
    eligible = [candidate for candidate in candidates if candidate.get('accountType') == account_type and candidate['id'] != profile.get('id') and intent_compatible(profile, candidate)]
    return sorted([{'candidate': candidate, **score(profile, candidate, reference)} for candidate in eligible], key=lambda item: (-item['compatibility'], item['candidate']['id']))
