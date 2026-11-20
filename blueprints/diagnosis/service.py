class DiagnosisService:
    @staticmethod
    def format_confidence(conf):
        return round(float(conf), 2)
