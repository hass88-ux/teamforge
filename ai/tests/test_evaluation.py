import unittest
from teamforge_ai.demo import PROFILES
from teamforge_ai.evaluation import split_profiles, fit, predict, ndcg, FEATURES

class EvaluationTests(unittest.TestCase):
    def test_profile_splits_are_disjoint_complete_and_repeatable(self):
        splits = split_profiles(PROFILES)
        ids = [set(p['id'] for p in part) for part in splits]
        self.assertEqual([len(part) for part in splits], [360, 120, 120])
        self.assertFalse(ids[0] & ids[1] or ids[0] & ids[2] or ids[1] & ids[2])
        self.assertEqual(len(set.union(*ids)), len(PROFILES))
        self.assertEqual(splits, split_profiles(list(reversed(PROFILES))))

    def test_training_learns_a_separable_signal(self):
        no = [0.] * len(FEATURES)
        yes = [1.] + [0.] * (len(FEATURES) - 1)
        model = fit([(no, 0), (yes, 1)] * 10, 160)
        self.assertLess(predict(model, no), .2)
        self.assertGreater(predict(model, yes), .8)

    def test_metric_handles_perfect_reverse_and_no_relevance(self):
        self.assertEqual(ndcg([3, 2, 1], [3, 2, 1]), 1)
        self.assertLess(ndcg([1, 2, 3], [3, 2, 1]), 1)
        self.assertIsNone(ndcg([0, 0], [0, 0]))
