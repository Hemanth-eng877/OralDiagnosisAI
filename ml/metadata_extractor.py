import os

def extract_image_metadata(path):
    stat = os.stat(path)
    return {'size_bytes': stat.st_size, 'modified': stat.st_mtime}
