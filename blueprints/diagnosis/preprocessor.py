import numpy as np

def normalize_pixel_tensor(img_arr):
    return np.asarray(img_arr, dtype=np.float32) / 255.0
