import unittest
from teamforge_ai.challenge import cases
from teamforge_ai.ranking import intent_compatible, score
from teamforge_ai.evaluation import REFERENCE


class ChallengeTest(unittest.TestCase):
    def test_fixture_labels_and_identity(self):
        rows = cases()
        self.assertEqual(len(rows), 12)
        self.assertEqual(len({c['name'] for c in rows}), len(rows))
        for case in rows:
            self.assertEqual(max(x['grade'] for x in case['candidates']), 3)
            self.assertTrue(any(x['grade'] < 3 for x in case['candidates']))
            self.assertTrue(all(not x['profile']['id'] == case['query']['id'] for x in case['candidates']))

    def test_timezone_case_compares_instants(self):
        case = next(c for c in cases() if c['name'] == 'different-timezone-same-instant')
        self.assertEqual([score(case['query'], x['profile'], REFERENCE)['overlapHours'] for x in case['candidates']], [2, 0])

    def test_incompatible_fixture_candidates_are_ineligible(self):
        for case in cases():
            for entry in case['candidates']:
                if not intent_compatible(case['query'], entry['profile']):
                    self.assertEqual(entry['grade'], 0)
