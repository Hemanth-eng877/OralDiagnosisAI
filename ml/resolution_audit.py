def audit_resolution(w, h):
    return {'valid': w >= 224 and h >= 224, 'w': w, 'h': h}
