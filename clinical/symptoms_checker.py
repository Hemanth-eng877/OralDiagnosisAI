def check_red_flag_symptoms(symptoms):
    red_flags = {'unexplained_bleeding', 'persistent_numbness', 'dysphagia', 'loose_teeth'}
    return bool(set(symptoms) & red_flags)
