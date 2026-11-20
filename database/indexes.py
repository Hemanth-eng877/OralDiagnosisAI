INDEX_LIST = [
    'CREATE INDEX IF NOT EXISTS idx_patients_name ON patients(full_name);',
    'CREATE INDEX IF NOT EXISTS idx_diag_created ON diagnoses(created_at);'
]
