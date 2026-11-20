class AuditLogEntry:
    def __init__(self, id, user_id, action, target_id, timestamp=None):
        self.id = id
        self.user_id = user_id
        self.action = action
        self.target_id = target_id
        self.timestamp = timestamp
