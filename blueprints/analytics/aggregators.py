from collections import Counter

def aggregate_monthly_trends(records):
    counts = Counter(r.get('created_at', '')[:7] for r in records if r.get('created_at'))
    return dict(sorted(counts.items()))
