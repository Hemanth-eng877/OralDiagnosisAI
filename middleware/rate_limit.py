import time
from collections import defaultdict

_request_history = defaultdict(list)

def is_rate_limited(client_ip, max_requests=60, window_secs=60):
    now = time.time()
    history = [t for t in _request_history[client_ip] if now - t < window_secs]
    _request_history[client_ip] = history
    if len(history) >= max_requests:
        return True
    history.append(now)
    return False
