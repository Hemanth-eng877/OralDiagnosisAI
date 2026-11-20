from datetime import datetime, timedelta

def calculate_next_followup(diagnosis, high_risk=False):
    days = 30 if diagnosis == 'OSCC' or high_risk else 180
    return (datetime.now() + timedelta(days=days)).strftime('%Y-%m-%d')
