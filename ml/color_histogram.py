import numpy as np

def compute_rgb_histogram(img_arr):
    return [np.histogram(img_arr[:,:,c], bins=16)[0].tolist() for c in range(3)]
