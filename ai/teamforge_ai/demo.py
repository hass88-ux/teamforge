import random
from .ranking import rank

ARCHETYPES = [
    ('Backend engineer', ['Java', 'Spring Boot', 'PostgreSQL'], ['Frontend engineer', 'Designer']),
    ('Frontend engineer', ['React', 'TypeScript', 'UI/UX'], ['Backend engineer', 'ML engineer']),
    ('Full-stack engineer', ['React', 'Python', 'PostgreSQL'], ['Designer', 'Founder']),
    ('ML engineer', ['Python', 'PyTorch', 'Data engineering'], ['Backend engineer', 'Frontend engineer']),
    ('Designer', ['UI/UX', 'Product'], ['Frontend engineer', 'Full-stack engineer']),
    ('Founder', ['Product', 'UI/UX'], ['Full-stack engineer', 'ML engineer']),
    ('Researcher', ['Python', 'PyTorch'], ['ML engineer', 'Data engineer']),
    ('Student', ['Python', 'React'], ['Backend engineer', 'Designer']),
    ('Mobile developer', ['Mobile', 'TypeScript'], ['Backend engineer', 'Designer']),
    ('DevOps engineer', ['AWS', 'Docker', 'Security'], ['Backend engineer', 'Full-stack engineer']),
    ('Data engineer', ['Data engineering', 'Python', 'PostgreSQL'], ['ML engineer', 'Researcher']),
    ('Cybersecurity engineer', ['Security', 'Python', 'Docker'], ['Backend engineer', 'DevOps engineer']),
]
DOMAINS = ['AI', 'Education', 'Healthcare', 'Climate', 'Fintech', 'Developer tools', 'Cybersecurity', 'Gaming', 'Robotics', 'Research', 'Consumer apps']
GOALS = ['Hackathon', 'Portfolio project', 'Open source', 'Research collaboration', 'Startup', 'Weekend project']
STYLES = ['Structured', 'Flexible', 'Fast-moving', 'Research-heavy', 'Design-first', 'Engineering-first', 'Product-first']
ZONES = ['America/New_York', 'America/Los_Angeles', 'Europe/London', 'Asia/Kolkata', 'Asia/Tokyo', 'UTC']
FIRST = ['Alex', 'Jordan', 'Sam', 'Taylor', 'Morgan', 'Casey', 'Riley', 'Avery', 'Quinn', 'Jamie', 'Drew', 'Sky', 'Robin', 'Devon', 'Sasha', 'Emery', 'Noah', 'Maya', 'Aria', 'Leo', 'Nora', 'Luca', 'Zara', 'Owen', 'Isla', 'Amir', 'Elena', 'Evan']
LAST = ['Kim', 'Rivera', 'Patel', 'Chen', 'Reed', 'Morgan', 'Park', 'Singh', 'Ellis', 'Ali', 'Lee', 'Nguyen', 'Cruz', 'Shah', 'Brooks', 'Lane', 'Stone', 'Wong', 'Ford', 'Bell', 'Diaz', 'Green']


def generate_profiles(count=600, seed=41):
    rng = random.Random(seed)
    result = []
    for index in range(count):
        role, base_skills, sought = ARCHETYPES[index % len(ARCHETYPES)]
        interests = rng.sample(DOMAINS, rng.randint(1, 4))
        hours = rng.choice([2, 5, 8, 12, 18, 25])
        selected_days = rng.sample(range(7), rng.randint(2, 5))
        selected_hours = rng.choice([[8, 9, 12], [18, 19, 20], [20, 21, 22], [10, 11, 12, 13]])
        result.append({
            'id': f'demo-{index:03d}', 'accountType': 'DEMO',
            'displayName': f'{FIRST[index % len(FIRST)]} {LAST[index // len(FIRST) % len(LAST)]}' if index % 5 else f'{interests[0]} Collective {index + 1}',
            'entityType': 'ORGANIZATION' if index % 5 == 0 else 'INDIVIDUAL',
            'matchingIntent': ['PROVIDER', 'SEEKER', 'COLLABORATOR'][index // len(ARCHETYPES) % 3],
            'description': f'{role} offering {", ".join(base_skills)} for {interests[0].lower()} projects.',
            'neededSkills': rng.sample(ARCHETYPES[(index + 1) % len(ARCHETYPES)][1], 1),
            'role': role, 'headline': f'{role} exploring {interests[0].lower()} projects',
            'skills': base_skills + rng.sample(['AWS', 'Docker', 'Product', 'TypeScript'], rng.randint(0, 2)),
            'interests': interests, 'rolesSought': sought, 'weeklyHours': hours,
            'goal': rng.choice(GOALS), 'workingStyle': rng.choice(STYLES), 'timezone': rng.choice(ZONES),
            'availability': [day * 24 + hour for day in selected_days for hour in selected_hours],
            'experienceLevel': rng.choice(['Learning', 'Intermediate', 'Experienced']),
            'projects': [{'name': f'{interests[0]} Studio', 'description': f'Exploring a {interests[0].lower()} prototype.', 'technologies': base_skills, 'synthetic': True}],
        })
    return result


PROFILES = generate_profiles()

# Browsing examples are isolated from reciprocal matches and persisted accounts.
SAMPLE_PROFILES = generate_profiles(800)
for index, person in enumerate(SAMPLE_PROFILES):
    first = (FIRST + ['Omer', 'Hassan', 'Aisha', 'Fatima', 'Sofia', 'Daniel', 'Hana', 'Yusuf'])[index % 36]
    last = (LAST + ['Mushtaq', 'Ahmed', 'Khan', 'Hussain'])[index // 36 % 26]
    person.update(id=f'sample-{index:03d}', displayName=f'{first} {last}', entityType='INDIVIDUAL', description=person['description'] + f" Project: {person['projects'][0]['name']}." + ' Demo account — generated profile; matches and messaging are unavailable.')

SAMPLE_PROFILES[0]['displayName'] = 'Omer Mushtaq'
SAMPLE_PROFILES[1]['displayName'] = 'Alex Lee'
SAMPLE_PROFILES[360]['displayName'] = 'Alex Kim'

def sample_page(offset=0):
    fields = ('id', 'displayName', 'entityType', 'matchingIntent', 'role', 'description', 'skills', 'neededSkills', 'interests', 'goal', 'weeklyHours')
    return {'accountType': 'SAMPLE', 'total': len(SAMPLE_PROFILES), 'offset': offset,
            'profiles': [{**{key: item[key] for key in fields}, 'accountType': 'DEMO'} for item in SAMPLE_PROFILES[offset:offset + 40]],
            'hasMore': offset + 40 < len(SAMPLE_PROFILES)}


def recommendations(profile):
    candidates = list(PROFILES)
    initial = rank(profile, candidates)
    if not initial or initial[0]['compatibility'] < 85:
        role = profile['rolesSought'][0]
        archetype = next((item for item in ARCHETYPES if item[0] == role), ARCHETYPES[0])
        complement = [skill for skill in archetype[1] if skill not in profile['skills']]
        if not complement: complement = ['Complementary project planning']
        candidates.append({**profile, 'id': 'demo-tailored', 'accountType': 'DEMO', 'displayName': 'Jamie Demo', 'role': role,
            'headline': 'A collaborator exploring your project interests', 'skills': complement,
            'rolesSought': [profile['role']], 'tailoredForDemo': True,
            'entityType': 'INDIVIDUAL',
            'matchingIntent': 'SEEKER' if profile.get('matchingIntent') == 'PROVIDER' else 'PROVIDER' if profile.get('matchingIntent') == 'SEEKER' else 'COLLABORATOR',
            'neededSkills': profile['skills'],
            'skills': list(dict.fromkeys(profile.get('neededSkills', []) + complement)),
            'description': 'Interested in combining our skills to build something together.',
            'projects': [{'name': f"{profile['interests'][0]} Together", 'description': 'A project exploring shared interests and complementary skills.', 'technologies': complement, 'synthetic': True}]})
    return [item for item in rank(profile, candidates) if item['compatibility'] > 50][:20]
