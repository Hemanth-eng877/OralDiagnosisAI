import uuid
from flask import request, g

def attach_request_id():
    g.request_id = request.headers.get('X-Request-ID', uuid.uuid4().hex[:12])
