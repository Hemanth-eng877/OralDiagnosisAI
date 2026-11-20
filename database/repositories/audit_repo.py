from database.repositories.base_repo import BaseRepository

class AuditRepository(BaseRepository):
    def record_event(self, user_id, action, target):
        self.conn.execute('INSERT INTO audit_logs (user_id, action, target_id) VALUES (?, ?, ?)', (user_id, action, target))
        self.conn.commit()
