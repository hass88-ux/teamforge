"""Conservative, English rule-based skill suggestions; never modifies a profile."""
import re

ALIASES = {
    'Java': r'java', 'Node.js': r'node(?:\.js|js)?', 'Spring Boot': r'spring\s+boot',
    'Python': r'python', 'PyTorch': r'pytorch', 'React': r'react(?:\.js|js)?',
    'TypeScript': r'typescript', 'PostgreSQL': r'postgres(?:ql)?',
    'AWS': r'aws|amazon web services', 'Docker': r'docker',
    'UI/UX': r'ui\s*/\s*ux|ux\s*/\s*ui|ui design|ux design',
    'Product': r'product management|product', 'Mobile': r'mobile development|mobile',
    'Security': r'cybersecurity|security', 'Data engineering': r'data engineering',
}
CUES = re.compile(r'\b(?P<needed>need(?:ed)?|looking for|seeking|require|want)|\b(?P<offered>offer|provide|know|can build with|experienced in|experience with|skilled in|proficient in)', re.I)
NEGATIVE = re.compile(r"\b(?:not|no|never|without|don['’]t|cannot|can['’]t|lack)\b", re.I)

def suggest(description):
    buckets = {'offered': set(), 'needed': set(), 'unclassified': set()}
    # Scope cues to clauses; a new direction overrides the preceding cue.
    for clause in re.split(r'[;\n!?]|\.\s+|\bbut\b', description, flags=re.I):
        if NEGATIVE.search(clause):
            continue
        cues = list(CUES.finditer(clause))
        for skill, pattern in ALIASES.items():
            for hit in re.finditer(r'(?<![\w])(?:' + pattern + r')(?![\w])', clause, re.I):
                preceding = [cue for cue in cues if cue.end() <= hit.start()]
                direction = preceding[-1].lastgroup if preceding else 'unclassified'
                buckets[direction].add(skill)
    return {**{key: [skill for skill in ALIASES if skill in value] for key, value in buckets.items()},
            'method': 'english-rules-v1'}
