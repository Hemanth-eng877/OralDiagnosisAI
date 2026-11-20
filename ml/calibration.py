def apply_temperature_scaling(logits, temperature=1.0):
    import numpy as np
    scaled = np.array(logits) / temperature
    exp = np.exp(scaled - np.max(scaled))
    return (exp / np.sum(exp)).tolist()
