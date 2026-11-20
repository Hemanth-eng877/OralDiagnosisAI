def compute_confidence_margin(scores):
    sorted_s = sorted(scores, reverse=True)
    if len(sorted_s) < 2: return 1.0
    return round(float(sorted_s[0] - sorted_s[1]), 4)
