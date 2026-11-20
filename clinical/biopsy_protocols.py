def determine_biopsy_urgency(risk_level, lesion_duration_weeks):
    if risk_level == 'High' or lesion_duration_weeks > 3:
        return 'Urgent (Within 7 Days)'
    return 'Routine (Within 14 Days)'
