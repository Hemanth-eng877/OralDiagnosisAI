import time

def time_query(conn, sql, params=()):
    t0 = time.time()
    cur = conn.execute(sql, params)
    rows = cur.fetchall()
    return rows, round((time.time() - t0) * 1000, 2)
