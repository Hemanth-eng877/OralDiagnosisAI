import re

def sanitize_phone(phone):
    if not phone: return ''
    return re.sub(r'[^0-9+-]', '', phone)
