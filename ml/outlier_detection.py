def is_luminance_outlier(mean_lum):
    return mean_lum < 20 or mean_lum > 240
