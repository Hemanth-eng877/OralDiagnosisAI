def evaluate_histological_margin(clearance_mm):
    if clearance_mm >= 5.0: return 'Clear'
    if clearance_mm >= 1.0: return 'Close'
    return 'Involved'
