def validate_aspect_ratio(w, h, max_ratio=2.5):
    ratio = max(w/h, h/w)
    return ratio <= max_ratio
