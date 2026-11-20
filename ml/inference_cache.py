class InferenceCache:
    def __init__(self):
        self._cache = {}
    def get(self, hash_key): return self._cache.get(hash_key)
    def set(self, hash_key, val): self._cache[hash_key] = val
