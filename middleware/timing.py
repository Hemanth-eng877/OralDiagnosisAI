import time
from flask import g

def start_timer():
    g.start_time = time.time()

def get_elapsed_ms():
    return round((time.time() - getattr(g, 'start_time', time.time())) * 1000, 2)
