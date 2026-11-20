from database.repositories.base_repo import BaseRepository

class DiagnosisRepository(BaseRepository):
    def list_by_patient(self, patient_id):
        cur = self.conn.execute('SELECT * FROM diagnoses WHERE patient_id = ? ORDER BY id DESC', (patient_id,))
        return cur.fetchall()
