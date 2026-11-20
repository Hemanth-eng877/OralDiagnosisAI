def validate_patient_payload(data):
    errors = []
    if not data.get('full_name'): errors.append('full_name required')
    age = data.get('age')
    if age is not None and (age < 0 or age > 130): errors.append('invalid age')
    return errors
