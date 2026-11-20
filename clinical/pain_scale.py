def interpret_vas_pain_score(score):
    if score >= 7: return 'Severe'
    if score >= 4: return 'Moderate'
    if score >= 1: return 'Mild'
    return 'None'
