def generate_dummy_heatmap(width=224, height=224):
    import numpy as np
    return np.zeros((height, width), dtype=np.float32)
