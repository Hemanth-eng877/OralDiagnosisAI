class BatchQueue:
    def __init__(self):
        self._queue = []
    def add(self, item): self._queue.append(item)
    def size(self): return len(self._queue)
