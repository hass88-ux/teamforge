"""Exercise only two freshly created disposable accounts, then delete them.

Never accepts existing user credentials or selects real members for interaction.
Usage: python scripts/acceptance-smoke.py https://teamforge-jq44.onrender.com
"""
import datetime
import http.cookiejar
import json
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


class Client:
    def __init__(self, base):
        self.base = base.rstrip('/')
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.email = f"release-qa-{uuid.uuid4().hex}@example.test"
        self.password = secrets.token_urlsafe(24)
        self.created = False

    def request(self, path, method='GET', body=None, expected=200):
        headers = {}
        if method != 'GET':
            token = self.request('/api/auth/csrf')
            headers[token['headerName']] = token['token']
        data = None if body is None else json.dumps(body).encode()
        if data is not None:
            headers['Content-Type'] = 'application/json'
        try:
            response = self.http.open(urllib.request.Request(self.base + path, data=data, headers=headers, method=method), timeout=150)
        except urllib.error.HTTPError as failure:
            response = failure
        with response:
            status = response.code
            if status != expected:
                raise RuntimeError(f'{method} {path}: expected {expected}, received {status}')
            raw = response.read()
            return json.loads(raw) if raw and response.headers.get_content_type() == 'application/json' else None

    def login(self):
        self.request('/api/auth/login', 'POST', {'email': self.email, 'password': self.password})

    def cleanup(self):
        if self.created:
            self.login()
            self.request('/api/account/delete', 'POST', {'password': self.password}, 204)
            self.created = False


def run(base):
    parsed = urllib.parse.urlsplit(base)
    if parsed.scheme != 'https' and parsed.hostname not in ('127.0.0.1', 'localhost'):
        raise ValueError('Use HTTPS for hosted acceptance checks.')
    a, b, anonymous = Client(base), Client(base), Client(base)
    clients = (a, b)
    try:
        assert anonymous.request('/actuator/health')['status'] == 'UP'
        anonymous.request('/api/account/export', expected=401)
        anonymous.request('/api/moderation/reports', expected=401)
        for client, intent, skills, needs, role, sought in (
            (a, 'PROVIDER', ['Java', 'Spring Boot'], ['React', 'TypeScript'], 'Backend engineer', 'Frontend engineer'),
            (b, 'SEEKER', ['React', 'TypeScript'], ['Java', 'Spring Boot'], 'Frontend engineer', 'Backend engineer'),
        ):
            user = client.request('/api/auth/signup', 'POST', {'email': client.email, 'password': client.password}, 201)
            client.created = True
            client.id = user['id']
            profile = dict(displayName='Temporary release QA', role=role, skills=skills, neededSkills=needs,
                           interests=['Education', 'Developer tools'], rolesSought=[sought], weeklyHours=10,
                           goal='Portfolio project', workingStyle='Structured', timezone='UTC', availability=[20, 44],
                           entityType='INDIVIDUAL', matchingIntent=intent,
                           description='Automated release check account — temporary; not an available collaborator.')
            saved = client.request('/api/profiles/me', 'PUT', profile)
            assert saved['discoverable'] is False
            client.request('/api/profiles/me/visibility', 'PUT', {'discoverable': True})
            assert client.request('/api/profiles/me')['profile']['skills'] == skills
            assert client.request('/api/moderation/access')['allowed'] is False
            client.request('/api/moderation/reports', expected=403)
        ranked = a.request('/api/discovery/recommendations')['recommendations']
        item = next(item for item in ranked if item['candidate']['id'] == b.id)
        assert item['compatibility'] > 50
        assert not {'email', 'timezone', 'availability', 'password'} & item['candidate'].keys()
        assert len(anonymous.request('/api/demo/samples?offset=760')['profiles']) == 40
        a.request('/api/discovery/decisions', 'POST', {'candidateId': 'sample-000', 'decision': 'LIKE'}, 400)
        assert a.request('/api/matches') == []
        assert a.request('/api/discovery/decisions', 'POST', {'candidateId': b.id, 'decision': 'LIKE'})['matched'] is False
        matched = b.request('/api/discovery/decisions', 'POST', {'candidateId': a.id, 'decision': 'LIKE'})
        assert matched['matched'] is True
        match = matched['matchId']
        message = {'clientId': str(uuid.uuid4()), 'text': 'Disposable release check message'}
        first = a.request(f'/api/matches/{match}/messages', 'POST', message)
        again = a.request(f'/api/matches/{match}/messages', 'POST', message)
        assert first['sequence'] == again['sequence']
        inbox = b.request(f'/api/matches/{match}/messages')
        assert len(inbox['messages']) == 1 and inbox['messages'][0]['text'] == message['text']
        b.request(f'/api/matches/{match}/read', 'POST', {'sequence': first['sequence']}, 204)
        assert b.request('/api/matches')[0]['unreadCount'] == 0
        proposal = a.request(f'/api/matches/{match}/proposals', 'POST', {
            'clientId': str(uuid.uuid4()), 'kind': 'Virtual coffee', 'timezone': 'UTC', 'note': 'Release check only',
            'startsAt': (datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=1)).isoformat()})
        assert b.request(f'/api/matches/{match}/proposals/{proposal["id"]}/response', 'POST', {'status': 'ACCEPTED'})['status'] == 'ACCEPTED'
        project = a.request('/api/projects', 'POST', {'clientId': str(uuid.uuid4()), 'name': 'Disposable release check', 'description': 'Temporary acceptance fixture', 'stage': 'Prototype'})
        pid = project['id']
        a.request(f'/api/projects/{pid}/members', 'POST', {'matchId': match})
        b.request(f'/api/projects/{pid}/response', 'POST', {'status': 'ACCEPTED'})
        task = b.request(f'/api/projects/{pid}/tasks', 'POST', {'clientId': str(uuid.uuid4()), 'title': 'Verify project task', 'kind': 'TASK'})
        completed = a.request(f'/api/projects/{pid}/tasks/{task["id"]}', 'PUT', {'done': True, 'revision': task['revision']})
        assert completed['done'] is True
        a.request(f'/api/projects/{pid}/tasks/{task["id"]}', 'PUT', {'done': False, 'revision': task['revision']}, 409)
        b.request('/api/safety/reports', 'POST', {'clientId': str(uuid.uuid4()), 'matchId': match, 'reason': 'Other', 'details': 'Disposable automated release check, not a complaint.'})
        exported = a.request('/api/account/export')
        assert 'password_hash' not in json.dumps(exported)
        b.request('/api/safety/blocks', 'POST', {'targetId': a.id}, 204)
        a.request(f'/api/matches/{match}/messages', expected=404)
        assert a.request('/api/matches') == []
        b.request(f'/api/projects/{pid}/leave', 'POST', expected=204)
        b.request(f'/api/projects/{pid}/tasks', expected=404)
        key = a.request('/api/account/recovery-key', 'POST', {'password': a.password})['key']
        new_password = secrets.token_urlsafe(24)
        anonymous.request('/api/auth/recover', 'POST', {'email': a.email, 'key': key, 'password': new_password}, 204)
        a.password = new_password
        a.request('/api/auth/me', expected=401)
        a.login()
        anonymous.request('/api/auth/recover', 'POST', {'email': a.email, 'key': key, 'password': new_password}, 400)
        print('PASS: signup, profile privacy, live ranking, mutual match, messages/retries, read cursor, coffee, projects/tasks, stale updates, reports/access, export, block, membership leave, recovery and session revocation.')
    finally:
        failed = False
        for client in clients:
            try:
                client.cleanup()
            except Exception:
                failed = True
                print('ERROR: disposable fixture cleanup failed; investigate before launch.', file=sys.stderr)
        if failed:
            raise RuntimeError('Fixture cleanup incomplete')
        print('PASS: disposable accounts and their related records removed.')


if __name__ == '__main__':
    run(sys.argv[1])
