def validate_email(email):
    return '@' in email and len(email) > 5

def validate_password_strength(password):
    return len(password) >= 6
