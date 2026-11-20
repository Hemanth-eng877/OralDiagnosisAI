import time

class InferenceTimer:
    def __enter__(self):
        self.t0 = time.time(); return self
    def __exit__(self, *args):
        self.elapsed_ms = (time.time() - self.t0) * 1000
