def evaluate_cervical_nodes(palpable, fixed):
    if fixed: return 'Suspect N2/N3'
    if palpable: return 'Suspect N1'
    return 'Clinically N0'
