import sqlite3

def run_migrations(conn):
    conn.execute('CREATE TABLE IF NOT EXISTS _migrations (id TEXT PRIMARY KEY)')
