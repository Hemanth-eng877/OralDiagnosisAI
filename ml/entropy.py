import numpy as np

def compute_prediction_entropy(probabilities):
    p = np.clip(probabilities, 1e-7, 1.0)
    return float(-np.sum(p * np.log2(p)))
