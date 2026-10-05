"""Run the private recommendation process and public API as one preview service."""
import os
import signal
import subprocess
import sys
import time
import urllib.request

children = []
stopping = False

def stop(_signal=None, _frame=None):
    global stopping
    stopping = True
    for child in children:
        if child.poll() is None:
            child.terminate()

signal.signal(signal.SIGTERM, stop)
signal.signal(signal.SIGINT, stop)

try:
    children.append(subprocess.Popen([
        sys.executable, '-m', 'uvicorn', 'teamforge_ai.app:app',
        '--host', '127.0.0.1', '--port', '8001',
    ], cwd='/app/ai'))
    ready = False
    for _ in range(60):
        if stopping or children[0].poll() is not None:
            break
        try:
            with urllib.request.urlopen('http://127.0.0.1:8001/health', timeout=1) as response:
                ready = response.status == 200
            if ready:
                break
        except OSError:
            time.sleep(0.5)
    if not ready:
        raise RuntimeError('Recommendation process failed to become ready')
    children.append(subprocess.Popen([
        'java', '-Xms64m', '-Xmx192m', '-XX:MaxMetaspaceSize=128m',
        '-XX:ReservedCodeCacheSize=48m', '-jar', '/app/teamforge.jar',
    ], env=os.environ.copy()))
    while not stopping and all(child.poll() is None for child in children):
        time.sleep(0.5)
    if not stopping:
        raise RuntimeError('A required service exited')
except Exception as failure:
    print(str(failure), file=sys.stderr)
    sys.exit(1)
finally:
    stop()
    for child in children:
        try:
            child.wait(timeout=10)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait()
