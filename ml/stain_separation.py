def estimate_hematoxylin_fraction(r, b):
    # High blue vs red proxy
    return float(b > r)
