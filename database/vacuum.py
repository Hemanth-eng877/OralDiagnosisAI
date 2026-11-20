def vacuum_database(conn):
    conn.execute('VACUUM')
