from flask import jsonify

def api_success(data=None, message='Success', status_code=200):
    payload = {'status': 'success', 'message': message}
    if data is not None: payload.update(data)
    return jsonify(payload), status_code

def api_error(message, status_code=400, errors=None):
    payload = {'status': 'error', 'message': message}
    if errors: payload['errors'] = errors
    return jsonify(payload), status_code
