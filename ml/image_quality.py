import numpy as np

def check_blur_metric(gray_img):
    # Simple gradient variance proxy
    return float(np.var(gray_img))
