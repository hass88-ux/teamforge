"""Independent, hand-labelled stress cases. Offline only; never changes live scoring."""
import json
from copy import deepcopy
from pathlib import Path
from .evaluation import REFERENCE, fit, signals, training_rows, split_profiles, predict, ndcg, candidates, relevance
from .demo import PROFILES
from .ranking import score, intent_compatible, MODEL_VERSION


def profile(identifier, **changes):
    value = dict(id=identifier, accountType='DEMO', role='Backend engineer',
                 skills=['Java', 'Spring Boot'], neededSkills=['React'],
                 interests=['Developer tools'], rolesSought=['Frontend engineer'],
                 weeklyHours=8, goal='Portfolio project', workingStyle='Structured',
                 timezone='UTC', availability=[18, 19], matchingIntent='COLLABORATOR',
                 description='I offer Java and need React.')
    value.update(changes)
    return value


def cases():
    """Grades express explicit scenario preferences, not the production score formula."""
    query = profile('query')
    good = profile('complement', role='Frontend engineer', skills=['React'],
                   neededSkills=['Java'], rolesSought=['Backend engineer'])
    rows = []
    def add(name, q, candidates, rationale):
        rows.append(dict(name=name, query=deepcopy(q),
                         candidates=[dict(profile=deepcopy(p), grade=g) for p, g in candidates],
                         rationale=rationale))
    add('complement-over-keyword-copy', query, [(good, 3), (profile('same-stack'), 0)],
        'The stated React need is fulfilled by the frontend collaborator, not another Java offer.')
    add('no-overlapping-hours', query, [(dict(good, role='Designer', interests=['Education']), 3), (dict(good, id='no-time', availability=[8, 9]), 0)],
        'This scenario requires working together in the selected hours.')
    add('commitment-mismatch', query, [(good, 3), (dict(good, id='too-busy', weeklyHours=40), 1)],
        'Eight hours per week is preferred over a forty-hour expectation.')
    add('provider-provider-exclusion', dict(query, matchingIntent='PROVIDER'),
        [(dict(good, id='seeker', matchingIntent='SEEKER'), 3),
         (dict(good, id='provider', matchingIntent='PROVIDER'), 0)],
        'Providers must not be paired with providers.')
    add('seeker-seeker-exclusion', dict(query, matchingIntent='SEEKER'),
        [(dict(good, id='provider', matchingIntent='PROVIDER'), 3),
         (dict(good, id='seeker', matchingIntent='SEEKER'), 0)],
        'Seekers must not be paired with seekers.')
    add('one-sided-role-interest', query, [(good, 3),
        (dict(good, id='one-sided', rolesSought=['ML engineer'], neededSkills=['PyTorch']), 1)],
        'Both sides should receive something they asked for.')
    add('different-timezone-same-instant', query,
        [(dict(good, id='utc-equivalent', timezone='America/New_York', availability=[14, 15]), 3),
         (dict(good, id='same-wall-hour', timezone='America/New_York', availability=[18, 19]), 0)],
        'The September reference week is UTC-4 in New York; compare instants, not clock labels.')
    add('description-keyword-trap', query, [(good, 3),
        (profile('keyword-stuffing', description='React React React Java Java Java'), 0)],
        'Mentioning React in prose does not establish an offered skill.')
    add('goal-alignment', query, [(good, 3), (dict(good, id='different-goal', goal='Hackathon'), 2)],
        'A portfolio collaborator is preferred, but a strong hackathon collaborator still has value.')
    add('missing-required-offer', query, [(good, 3),
        (dict(good, id='wrong-offer', skills=['UI/UX'], neededSkills=['Java']), 1)],
        'A suitable role title alone does not supply the requested React skill.')
    add('collaborator-can-connect-to-provider', query,
        [(dict(good, id='available-provider', matchingIntent='PROVIDER'), 3),
         (profile('unrelated', skills=['PyTorch'], neededSkills=['Mobile'], role='ML engineer'), 0)],
        'Collaborator intent can connect to providers when complementary needs are met.')
    add('partial-availability', dict(query, availability=[18, 19, 20, 21]),
        [(dict(good, id='full-overlap', availability=[18, 19, 20, 21]), 3),
         (dict(good, id='partial-overlap', availability=[18, 8, 9, 10]), 2)],
        'More shared selected hours are preferred; partial overlap is still useful.')
    return rows


def report():
    train, validation, _ = split_profiles(PROFILES)
    # Select epochs on generated validation profiles, never on the challenge suite.
    rows = training_rows(train)
    options = [fit(rows, epochs) for epochs in (40, 80, 160)]
    # These features do not depend on epoch count; calculate them only once.
    validation_queries = []
    for query in validation:
        entries = [(p['id'], relevance(query, p), signals(query, p)) for p in candidates(query, validation)]
        ideal = sorted((entry[1] for entry in entries), reverse=True)
        if ideal and ideal[0] > 0: validation_queries.append((entries, ideal))
    def validation_metric(candidate_model):
        values = []
        for entries, ideal in validation_queries:
            ordered = sorted(entries, key=lambda e: (-predict(candidate_model, e[2]), e[0]))
            values.append(ndcg([e[1] for e in ordered], ideal))
        return round(sum(values) / len(values), 6) if values else 0
    model = max(options, key=validation_metric)
    output = {name: [] for name in ('weighted', 'learned', 'availability_guard')}
    for case in cases():
        eligible = [x for x in case['candidates'] if intent_compatible(case['query'], x['profile'])]
        ideal = sorted([x['grade'] for x in eligible], reverse=True)
        for name in output:
            def value(entry):
                result = score(case['query'], entry['profile'], REFERENCE)
                if name == 'learned': return predict(model, signals(case['query'], entry['profile']))
                if name == 'availability_guard' and not result['overlapHours']: return -1
                return result['compatibility']
            ordered = sorted(eligible, key=lambda entry: (-value(entry), entry['profile']['id']))
            output[name].append(dict(case=case['name'], topCandidate=ordered[0]['profile']['id'],
                topGrade=ordered[0]['grade'], ndcgAt2=ndcg([e['grade'] for e in ordered], ideal, 2),
                eligibleCandidateCount=len(eligible),
                rationale=case['rationale']))
    return dict(dataset='12 hand-labelled synthetic challenge cases; two candidates per query',
        referenceWeek=REFERENCE.date().isoformat(), scorerVersion=MODEL_VERSION, selectedEpochs=model['epochs'],
        training='Generated training profiles only; epoch selection uses generated validation profiles. Challenge cases are never used for fitting or selection.',
        results={name: dict(meanNdcgAt2=round(sum(c['ndcgAt2'] for c in rows)/len(rows), 6),
            topChoiceBestGrade=sum(c['topGrade'] == max(x['grade'] for x in case['candidates']) for c, case in zip(rows, cases())),
            rankingCases=sum(c['eligibleCandidateCount'] > 1 for c in rows),
            rankingOnlyMeanNdcgAt2=round(sum(c['ndcgAt2'] for c in rows if c['eligibleCandidateCount'] > 1)/sum(c['eligibleCandidateCount'] > 1 for c in rows), 6),
            cases=rows) for name, rows in output.items()},
        promotion='NONE: no learned model promotion. Weighted v2.1 corrects shared calendar-week alignment only; weights and eligibility are unchanged.',
        limitations=['Small authored diagnostic suite, not independent human judgement or a statistical generalization estimate.',
                     'Cases reuse a base profile; they are not independent samples and contain no real-user outcomes.',
                     'Availability guard is a diagnostic ablation designed for these cases, not a validated model improvement.',
                     'No description embeddings, language-model inference, or probability calibration.'])


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', default='../docs/evaluation/challenge-report.json')
    args = parser.parse_args()
    result = report()
    path = Path(args.output); path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({name: {k: v for k, v in r.items() if k != 'cases'} for name, r in result['results'].items()}, indent=2))
