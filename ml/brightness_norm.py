from PIL import ImageEnhance

def normalize_brightness(img, factor=1.05):
    return ImageEnhance.Brightness(img).enhance(factor)
