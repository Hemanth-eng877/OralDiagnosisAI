import sqlite3

def get_db(db_path='database.db'):
    c = sqlite3.connect(db_path)
    c.row_factory = sqlite3.Row
    return c
