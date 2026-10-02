import unittest
from pydantic import ValidationError
from teamforge_ai.app import DescriptionRequest, description_skills
from teamforge_ai.description import suggest

class DescriptionTests(unittest.TestCase):
    def test_directions_and_aliases_are_scoped(self):
        result = suggest('I offer Java, React and Node.js; I need Python and Postgres.')
        self.assertEqual(result['offered'], ['Java', 'Node.js', 'React'])
        self.assertEqual(result['needed'], ['Python', 'PostgreSQL'])

    def test_negation_and_word_boundaries(self):
        result = suggest("I don't know React but I offer Java. I need JavaScript. I am not looking for Python.")
        self.assertEqual(result['offered'], ['Java'])
        self.assertEqual(result['needed'], [])
        self.assertEqual(result['unclassified'], [])

    def test_ambiguous_mentions_and_changed_direction(self):
        result = suggest('React, Python. I know Java and need Docker. I offer Java.')
        self.assertEqual(result['unclassified'], ['Python', 'React'])
        self.assertEqual(result['offered'], ['Java'])
        self.assertEqual(result['needed'], ['Docker'])

    def test_endpoint_bounds(self):
        for text in ['', 'x' * 1001]:
            with self.assertRaises(ValidationError): DescriptionRequest(description=text)
        result = description_skills(DescriptionRequest(description='I need AWS and UI/UX'))
        self.assertEqual(result['needed'], ['AWS', 'UI/UX'])
        self.assertEqual(result['method'], 'english-rules-v1')