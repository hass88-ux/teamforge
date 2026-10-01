import unittest
from teamforge_ai.teams import recommend_team
from test_ranking import builder, REFERENCE

class TeamTests(unittest.TestCase):
    def project(self): return {'rolesNeeded': ['Frontend engineer', 'Designer'], 'weeklyHours': 8, 'domain': 'Education', 'technologies': ['Java', 'React']}
    def candidates(self):
        owner = builder()
        return [{**owner, 'id': 'front', 'accountType': 'DEMO', 'role': 'Frontend engineer', 'skills': ['React'], 'rolesSought': ['Backend engineer']},
                {**owner, 'id': 'design', 'accountType': 'DEMO', 'role': 'Designer', 'skills': ['UI/UX'], 'rolesSought': ['Backend engineer']}]
    def test_role_coverage_uniqueness_and_score(self):
        result = recommend_team(builder(), self.project(), self.candidates(), REFERENCE)
        self.assertTrue(result['feasible'])
        self.assertEqual(set(result['rolesCovered']), {'Frontend engineer', 'Designer'})
        self.assertEqual(len({p['id'] for p in result['members']}), 2)
        self.assertEqual(round(sum(result['contributions'].values())), result['teamScore'])
        self.assertEqual(result['sharedHours'], 2)
    def test_excludes_real_and_undercommitted_candidates(self):
        candidates = self.candidates()
        candidates[0]['accountType'] = 'REAL'
        self.assertFalse(recommend_team(builder(), self.project(), candidates, REFERENCE)['feasible'])
        candidates[0]['accountType'] = 'DEMO'; candidates[0]['weeklyHours'] = 2
        self.assertFalse(recommend_team(builder(), self.project(), candidates, REFERENCE)['feasible'])
    def test_owner_commitment_is_a_constraint(self):
        self.assertFalse(recommend_team({**builder(), 'weeklyHours': 2}, self.project(), self.candidates(), REFERENCE)['feasible'])
    def test_reports_missing_role_and_no_fabricated_team(self):
        result = recommend_team(builder(), {**self.project(), 'rolesNeeded': ['Astronaut']}, self.candidates(), REFERENCE)
        self.assertEqual(result['missingRoles'], ['Astronaut'])
        self.assertEqual(result['members'], [])
    def test_prefers_more_compatible_team_with_full_pool(self):
        good = self.candidates()
        bad = [{**p, 'id': p['id'] + '-bad', 'interests': ['Gaming'], 'availability': [8], 'goal': 'Startup'} for p in good]
        result = recommend_team(builder(), self.project(), bad + good, REFERENCE)
        self.assertEqual({p['id'] for p in result['members']}, {'front', 'design'})
