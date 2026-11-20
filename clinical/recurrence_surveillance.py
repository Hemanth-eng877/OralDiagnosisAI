def surveillance_frequency_months(year_post_treatment):
    if year_post_treatment <= 1: return 1
    if year_post_treatment <= 2: return 2
    return 6
