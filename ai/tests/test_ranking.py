import unittest
from datetime import datetime, timezone
from teamforge_ai.demo import PROFILES, recommendations
from teamforge_ai.ranking import score, rank, utc_slots, intent_compatible

REFERENCE = datetime(2026, 9, 30, tzinfo=timezone.utc)

def builder():
    return {'displayName': 'Test', 'role': 'Backend engineer', 'skills': ['Java'], 'interests': ['Education'], 'rolesSought': ['Frontend engineer'], 'weeklyHours': 8, 'goal': 'Portfolio project', 'workingStyle': 'Structured', 'timezone': 'America/New_York', 'availability': [20, 44]}

class RankingTests(unittest.TestCase):
    def test_diverse_synthetic_dataset(self):
        self.assertEqual(len(PROFILES), 600)
        self.assertEqual(len({p['id'] for p in PROFILES}), 600)
        self.assertGreaterEqual(len({p['role'] for p in PROFILES}), 12)
        self.assertTrue(all(p['accountType'] == 'DEMO' for p in PROFILES))

    def test_demo_guarantee_for_every_archetype(self):
        for profile in PROFILES[:36]:
            results = recommendations(profile)
            self.assertGreaterEqual(results[0]['compatibility'], 85)
            self.assertTrue(all(r['candidate']['accountType'] == 'DEMO' for r in results))

    def test_intent_matrix(self):
        for left in ['PROVIDER', 'SEEKER', 'COLLABORATOR']:
            for right in ['PROVIDER', 'SEEKER', 'COLLABORATOR']:
                a, b = {**builder(), 'matchingIntent': left}, {**builder(), 'matchingIntent': right, 'id': 'other', 'accountType': 'DEMO'}
                expected = not (left == right and left != 'COLLABORATOR')
                self.assertEqual(intent_compatible(a, b), expected)
                self.assertEqual(bool(rank(a, [b])), expected)

    def test_offered_java_meets_needed_java(self):
        a = {**builder(), 'matchingIntent': 'PROVIDER'}
        b = {**builder(), 'matchingIntent': 'SEEKER', 'neededSkills': ['Java', 'Python']}
        without_java = {**a, 'skills': ['React']}
        self.assertGreater(score(a, b, REFERENCE)['compatibility'], score(without_java, b, REFERENCE)['compatibility'])
        self.assertTrue(any('Java' in text and 'needs' in text for text in score(a, b, REFERENCE)['evidence']))

    def test_demo_threshold_and_intents(self):
        for intent in ['PROVIDER', 'SEEKER', 'COLLABORATOR']:
            profile = {**builder(), 'matchingIntent': intent, 'neededSkills': ['Python']}
            results = recommendations(profile)
            self.assertTrue(results)
            self.assertTrue(all(r['compatibility'] > 50 and intent_compatible(profile, r['candidate']) for r in results))
        self.assertEqual({p['entityType'] for p in PROFILES}, {'INDIVIDUAL', 'ORGANIZATION'})
        self.assertEqual(len({p['displayName'] for p in PROFILES}), 600)

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
