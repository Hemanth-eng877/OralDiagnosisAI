def verify_consent(record):
    return bool(record.get('has_consent_signed', False))
