import unittest
from datetime import datetime, timezone
from teamforge_ai.demo import PROFILES, recommendations
from teamforge_ai.ranking import score, rank, utc_slots

REFERENCE = datetime(2026, 9, 30, tzinfo=timezone.utc)

def builder():
    return {'displayName': 'Test', 'role': 'Backend engineer', 'skills': ['Java'], 'interests': ['Education'], 'rolesSought': ['Frontend engineer'], 'weeklyHours': 8, 'goal': 'Portfolio project', 'workingStyle': 'Structured', 'timezone': 'America/New_York', 'availability': [20, 44]}

class RankingTests(unittest.TestCase):
    def test_diverse_synthetic_dataset(self):
        self.assertEqual(len(PROFILES), 264)
        self.assertEqual(len({p['id'] for p in PROFILES}), 264)
        self.assertGreaterEqual(len({p['role'] for p in PROFILES}), 12)
        self.assertTrue(all(p['accountType'] == 'DEMO' for p in PROFILES))

    def test_demo_guarantee_for_every_archetype(self):
        for profile in PROFILES:
            results = recommendations(profile)
            self.assertGreaterEqual(results[0]['compatibility'], 85)
            self.assertTrue(all(r['candidate']['accountType'] == 'DEMO' for r in results))

    def test_reciprocal_symmetry_and_bounds(self):
        a, b = PROFILES[0], PROFILES[1]
        ab, ba = score(a, b, REFERENCE), score(b, a, REFERENCE)
        self.assertEqual(ab['compatibility'], ba['compatibility'])
        self.assertGreaterEqual(ab['compatibility'], 0)
        self.assertLessEqual(ab['compatibility'], 100)
        self.assertAlmostEqual(sum(ab['contributions'].values()), ab['forwardScore'], places=2)

    def test_timezone_alignment(self):
        a = builder()
        b = {**a, 'timezone': 'UTC', 'availability': [24, 48]}
        self.assertEqual(utc_slots(a, REFERENCE), utc_slots(b, REFERENCE))
        self.assertEqual(score(a, b, REFERENCE)['overlapHours'], 2)

    def test_excludes_self_and_real_profiles(self):
        a = {**builder(), 'id': 'self'}
        self.assertEqual(rank(a, [{**a, 'accountType': 'DEMO'}, {**a, 'id': 'real', 'accountType': 'REAL'}]), [])

    def test_incompatible_candidate_scores_lower(self):
        a = builder()
        good = {**a, 'role': 'Frontend engineer', 'skills': ['React'], 'rolesSought': ['Backend engineer']}
        bad = {**good, 'role': 'Researcher', 'rolesSought': ['Founder'], 'skills': ['Java'], 'interests': ['Climate'], 'availability': [8], 'goal': 'Startup', 'workingStyle': 'Research-heavy', 'weeklyHours': 25}
        self.assertGreater(score(a, good, REFERENCE)['compatibility'], score(a, bad, REFERENCE)['compatibility'])

if __name__ == '__main__': unittest.main()
