from PIL import ImageEnhance

def adjust_contrast(img, factor=1.2):
    enhancer = ImageEnhance.Contrast(img)
    return enhancer.enhance(factor)
