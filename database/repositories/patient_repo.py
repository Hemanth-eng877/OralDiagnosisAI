from database.repositories.base_repo import BaseRepository

class PatientRepository(BaseRepository):
    def list_by_user(self, user_id):
        cur = self.conn.execute('SELECT * FROM patients WHERE user_id = ? ORDER BY id DESC', (user_id,))
        return cur.fetchall()
