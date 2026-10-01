import unittest
from pydantic import ValidationError
from teamforge_ai.app import RealRequest, real_recommendations
from test_ranking import builder

class RealDiscoveryTests(unittest.TestCase):
    def owner(self):
        return {**builder(), 'id': 'owner', 'accountType': 'REAL', 'matchingIntent': 'PROVIDER'}

    def candidate(self):
        return {**builder(), 'id': 'candidate', 'accountType': 'REAL', 'role': 'Frontend engineer', 'skills': ['React'], 'neededSkills': ['Java'], 'rolesSought': ['Backend engineer'], 'matchingIntent': 'SEEKER'}

    def test_real_ranking_uses_same_intent_rules_and_no_demo_fallback(self):
        request = RealRequest(profile=self.owner(), candidates=[self.candidate(), {**self.candidate(), 'id': 'bad', 'matchingIntent': 'PROVIDER'}, self.owner()])
        result = real_recommendations(request)
        self.assertEqual(result['accountType'], 'REAL')
        self.assertEqual([r['candidate']['id'] for r in result['recommendations']], ['candidate'])
        self.assertGreater(result['recommendations'][0]['compatibility'], 50)
        self.assertEqual(real_recommendations(RealRequest(profile=self.owner(), candidates=[]))['recommendations'], [])

    def test_real_endpoint_rejects_demo_records_and_large_pools(self):
        for candidates in [[{**self.candidate(), 'accountType': 'DEMO'}], [self.candidate()] * 201]:
            with self.assertRaises(ValidationError): RealRequest(profile=self.owner(), candidates=candidates)

    def test_below_threshold_is_not_shown(self):
        poor = {**self.candidate(), 'interests': ['Gaming'], 'availability': [8], 'role': 'Designer', 'rolesSought': ['Founder'], 'workingStyle': 'Design-first', 'weeklyHours': 60, 'goal': 'Research collaboration', 'neededSkills': ['Python']}
        self.assertEqual(real_recommendations(RealRequest(profile=self.owner(), candidates=[poor]))['recommendations'], [])
