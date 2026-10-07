import unittest
from teamforge_ai.demo import SAMPLE_PROFILES, sample_page

class SampleTests(unittest.TestCase):
    def test_unique_names_disclosed_descriptions_and_isolated_identifiers(self):
        self.assertEqual(len(SAMPLE_PROFILES), 800)
        self.assertEqual(len({p['displayName'] for p in SAMPLE_PROFILES}), 800)
        self.assertTrue(all(p['id'].startswith('sample-') and 'Demo account' in p['description'] for p in SAMPLE_PROFILES))
        self.assertEqual(SAMPLE_PROFILES[0]['displayName'], 'Omer Mushtaq')

    def test_pages_cover_pool_without_repetition_or_private_schedule(self):
        pages = [sample_page(i) for i in range(0, 800, 40)]
        profiles = [p for page in pages for p in page['profiles']]
        self.assertEqual(len({p['id'] for p in profiles}), 800)
        self.assertFalse(pages[-1]['hasMore'])
        self.assertFalse(sample_page(800)['profiles'])
        self.assertTrue(all('availability' not in p and 'timezone' not in p and p['accountType'] == 'DEMO' for p in profiles))
