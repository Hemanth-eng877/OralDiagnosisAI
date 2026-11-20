def paginate_query(query, page=1, per_page=20):
    offset = (page - 1) * per_page
    return f'{query} LIMIT {per_page} OFFSET {offset}'
