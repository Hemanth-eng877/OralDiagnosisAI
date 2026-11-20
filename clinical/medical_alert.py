def check_critical_alerts(diagnosis, stage):
    return diagnosis == 'OSCC' and 'IV' in stage
