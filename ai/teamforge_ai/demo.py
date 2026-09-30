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
FIRST = ['Alex', 'Jordan', 'Sam', 'Taylor', 'Morgan', 'Casey', 'Riley', 'Avery', 'Quinn', 'Jamie', 'Drew', 'Sky']
LAST = ['Kim', 'Rivera', 'Patel', 'Chen', 'Reed', 'Morgan', 'Park', 'Singh', 'Ellis', 'Ali', 'Lee', 'Nguyen', 'Cruz', 'Shah', 'Brooks', 'Lane', 'Stone', 'Wong', 'Ford', 'Bell', 'Diaz', 'Green']


def generate_profiles(count=264, seed=41):
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
            'displayName': f'{FIRST[index % len(FIRST)]} {LAST[index // len(FIRST) % len(LAST)]}',
            'role': role, 'headline': f'{role} exploring {interests[0].lower()} projects',
            'skills': base_skills + rng.sample(['AWS', 'Docker', 'Product', 'TypeScript'], rng.randint(0, 2)),
            'interests': interests, 'rolesSought': sought, 'weeklyHours': hours,
            'goal': rng.choice(GOALS), 'workingStyle': rng.choice(STYLES), 'timezone': rng.choice(ZONES),
            'availability': [day * 24 + hour for day in selected_days for hour in selected_hours],
            'experienceLevel': rng.choice(['Learning', 'Intermediate', 'Experienced']),
            'projects': [{'name': f'{interests[0]} Studio', 'description': f'A fictional {interests[0].lower()} prototype for the demo.', 'technologies': base_skills, 'synthetic': True}],
        })
    return result


PROFILES = generate_profiles()


def recommendations(profile):
    candidates = list(PROFILES)
    initial = rank(profile, candidates)
    if not initial or initial[0]['compatibility'] < 85:
        role = profile['rolesSought'][0]
        archetype = next((item for item in ARCHETYPES if item[0] == role), ARCHETYPES[0])
        complement = [skill for skill in archetype[1] if skill not in profile['skills']]
        if not complement: complement = ['Complementary project planning']
        candidates.append({**profile, 'id': 'demo-tailored', 'accountType': 'DEMO', 'displayName': 'Jamie Demo', 'role': role,
            'headline': 'A fictional collaborator tailored to your demo preferences', 'skills': complement,
            'rolesSought': [profile['role']], 'tailoredForDemo': True,
            'projects': [{'name': f"{profile['interests'][0]} Together", 'description': 'A synthetic project example generated for this demo.', 'technologies': complement, 'synthetic': True}]})
    return rank(profile, candidates)[:20]
