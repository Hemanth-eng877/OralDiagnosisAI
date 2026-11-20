def triage_telehealth_consult(urgency):
    return 'Immediate' if 'Urgent' in urgency else 'Scheduled'
