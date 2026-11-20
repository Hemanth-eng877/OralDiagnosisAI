import numpy as np

def estimate_edge_density(gray_arr):
    # Simple Sobel proxy
    dx = np.diff(gray_arr, axis=1)
    return float(np.mean(np.abs(dx)))
