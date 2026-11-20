def requires_second_opinion(confidence, diagnosis):
    # If model is uncertain (<85%), flag for pathologist manual review
    return confidence < 85.0
