def calculate_oscc_ratio(total_reports, oscc_count):
    if not total_reports: return 0.0
    return round((oscc_count / total_reports) * 100.0, 2)
