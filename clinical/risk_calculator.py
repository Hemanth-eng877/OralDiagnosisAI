def calculate_malignancy_risk(age, tobacco, alcohol, ulceration):
    score = 0
    if age > 50: score += 2
    if tobacco: score += 4
    if alcohol: score += 2
    if ulceration: score += 5
    return 'High' if score >= 8 else ('Moderate' if score >= 4 else 'Low')
