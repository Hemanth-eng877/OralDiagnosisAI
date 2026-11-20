from contextlib import contextmanager

@contextmanager
def transaction_scope(conn):
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
