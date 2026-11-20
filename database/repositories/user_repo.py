from database.repositories.base_repo import BaseRepository

class UserRepository(BaseRepository):
    def get_by_email(self, email):
        cur = self.conn.execute('SELECT * FROM users WHERE email = ?', (email,))
        return cur.fetchone()
