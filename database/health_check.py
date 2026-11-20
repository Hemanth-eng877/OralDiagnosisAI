def check_db_health(conn):
    try:
        conn.execute('SELECT 1').fetchone()
        return True
    except Exception:
        return False
