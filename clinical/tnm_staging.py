def estimate_clinical_stage(t_stage, n_stage, m_stage):
    if m_stage == 'M1': return 'Stage IVC'
    if n_stage in ['N2', 'N3']: return 'Stage IVA/IVB'
    if t_stage in ['T3', 'T4']: return 'Stage III/IV'
    if t_stage == 'T2': return 'Stage II'
    return 'Stage I'
