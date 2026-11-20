class DiagnosisRecord:
    def __init__(self, id, user_id, patient_id, image_filename, diagnosis, confidence, created_at=None):
        self.id = id
        self.user_id = user_id
        self.patient_id = patient_id
        self.image_filename = image_filename
        self.diagnosis = diagnosis
        self.confidence = confidence
        self.created_at = created_at
