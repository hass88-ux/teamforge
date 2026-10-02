"""Offline synthetic benchmark. Never used by the live recommendation endpoints."""
import json
import math
import random
from datetime import datetime, timezone
from pathlib import Path
from .demo import PROFILES
from .ranking import WEIGHTS, directional, intent_compatible, score

REFERENCE = datetime(2026, 9, 28, tzinfo=timezone.utc)
FEATURES = list(WEIGHTS)

def split_profiles(profiles, seed=42):
    ordered = sorted(profiles, key=lambda p: p['id'])
    random.Random(seed).shuffle(ordered)
    first, second = int(len(ordered) * .6), int(len(ordered) * .8)
    return ordered[:first], ordered[first:second], ordered[second:]

def signals(a, b):
    _, forward, _ = directional(a, b, REFERENCE)
    _, reverse, _ = directional(b, a, REFERENCE)
    return [min(forward[key], reverse[key]) / WEIGHTS[key] for key in FEATURES]

def relevance(a, b):
    """Hand-authored synthetic preference, not clicks or successful partnerships."""
    x = signals(a, b)
    role, skill, interest, availability, commitment, goal, _ = x
    if availability == 0 or commitment < .5:
        return 0
    if role == 1 and skill >= .5 and (interest > 0 or goal == 1):
        return 3
    if (role == 1 or skill >= .5) and interest > 0:
        return 2
    return 1 if skill > 0 or interest > 0 else 0

def sigmoid(value):
    return 1 / (1 + math.exp(-max(-40, min(40, value))))

def predict(model, x):
    return sigmoid(model['bias'] + sum(w * v for w, v in zip(model['weights'], x)))

def fit(rows, epochs=80):
    weights, bias = [0.] * len(FEATURES), 0.
    if not rows:
        raise ValueError('Training pairs required')
    for _ in range(epochs):
        gradients, bias_gradient = [0.] * len(FEATURES), 0.
        for x, label in rows:
            error = sigmoid(bias + sum(w * v for w, v in zip(weights, x))) - label
            bias_gradient += error
            for i, value in enumerate(x):
                gradients[i] += error * value
        rate = .8
        bias -= rate * bias_gradient / len(rows)
        weights = [w - rate * (g / len(rows) + .001 * w) for w, g in zip(weights, gradients)]
    return {'features': FEATURES, 'weights': weights, 'bias': bias, 'epochs': epochs}

def candidates(query, pool):
    return [p for p in pool if p['id'] != query['id'] and intent_compatible(query, p)]

def training_rows(pool):
    rng = random.Random(17)
    rows = []
    for query in pool:
        eligible = candidates(query, pool)
        for candidate in rng.sample(eligible, min(30, len(eligible))):
            rows.append((signals(query, candidate), int(relevance(query, candidate) >= 2)))
    return rows

def ndcg(grades, ideal, k=5):
    def dcg(values):
        return sum((2 ** grade - 1) / math.log2(i + 2) for i, grade in enumerate(values[:k]))
    denominator = dcg(ideal)
    return dcg(grades) / denominator if denominator else None

def evaluate(pool, model):
    totals = {key: [] for key in ('weighted', 'learned', 'random')}
    no_relevance = 0
    rng = random.Random(23)
    for query in pool:
        eligible = candidates(query, pool)
        entries = [(p['id'], relevance(query, p), score(query, p, REFERENCE)['compatibility'], predict(model, signals(query, p)), rng.random()) for p in eligible]
        ideal = sorted((e[1] for e in entries), reverse=True)
        if not ideal or ideal[0] == 0:
            no_relevance += 1
            continue
        for key, column in [('weighted', 2), ('learned', 3), ('random', 4)]:
            ordered = sorted(entries, key=lambda e: (-e[column], e[0]))
            totals[key].append(ndcg([e[1] for e in ordered], ideal))
    return {'queries': len(pool), 'queriesWithoutRelevantCandidates': no_relevance,
            'ndcgAt5': {key: round(sum(values) / len(values), 6) if values else None for key, values in totals.items()}}

def run(output):
    train, validation, test = split_profiles(PROFILES)
    rows = training_rows(train)
    # Select epoch count on validation only; inspect held-out test exactly afterward.
    options = [fit(rows, epochs) for epochs in (40, 80, 160)]
    validation_results = [evaluate(validation, model) for model in options]
    best = max(range(len(options)), key=lambda i: validation_results[i]['ndcgAt5']['learned'])
    model = options[best]
    report = {'dataset': '600 deterministic synthetic demo profiles', 'seed': 42,
              'referenceWeek': REFERENCE.date().isoformat(), 'split': {'train': len(train), 'validation': len(validation), 'test': len(test)},
              'splitPolicy': 'Profile-disjoint; pairs and candidate pools never cross splits. No tailored demo candidates.',
              'trainPairs': len(rows), 'positiveTrainPairs': sum(y for _, y in rows),
              'validationEpochTrials': [{'epochs': m['epochs'], **r} for m, r in zip(options, validation_results)],
              'selectedEpochs': model['epochs'], 'test': evaluate(test, model),
              'limitations': ['Labels are hand-authored synthetic preferences, not human decisions.', 'Metrics measure agreement with this generator, not real-world matching quality.', 'No probability calibration or deployment claim.', 'No live model promotion; production keeps weighted-reciprocal-v2.']}
    output = Path(output); output.mkdir(parents=True, exist_ok=True)
    (output / 'synthetic-ranking-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    (output / 'synthetic-ranking-model.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
    return report

if __name__ == '__main__':
    print(json.dumps(run(Path(__file__).resolve().parents[1] / 'artifacts'), indent=2))
