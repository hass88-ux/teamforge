# TeamForge recommendation service

Requires Python 3.11+ (verified locally with 3.14). From this directory:

```sh
python -m venv .venv
# Activate .venv for your shell, then:
python -m pip install -r requirements.txt
python -m unittest discover -s tests -v
python -m uvicorn teamforge_ai.app:app --host 127.0.0.1 --port 8001
```

`GET /health` returns the model version. `POST /recommendations/demo` accepts the profile contract and returns 20 scored demo candidates. This is a private internal service: expose only the Java API publicly. Production needs bounded request sizes, request rate controls, and service network isolation before launch.

No paid APIs, neural inference, database, or GPU are needed for this baseline. All profiles and project history are synthetic. See `docs/recommendation-system.md` in the repository root for weights, semantics, limitations, and disclosure.
