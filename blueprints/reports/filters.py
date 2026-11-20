def filter_reports_by_range(reports, min_conf=0.0, max_conf=100.0):
    return [r for r in reports if min_conf <= float(r.get('confidence', 0)) <= max_conf]
