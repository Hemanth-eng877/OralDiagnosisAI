import numpy as np

def compute_channel_means(img_arr):
    return [float(np.mean(img_arr[:,:,c])) for c in range(3)]
