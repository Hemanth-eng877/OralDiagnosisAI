def compute_uw_qol_score(subscores):
    return sum(subscores) / max(len(subscores), 1)
