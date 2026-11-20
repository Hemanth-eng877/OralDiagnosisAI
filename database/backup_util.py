import shutil

def backup_sqlite_db(source, dest):
    shutil.copy2(source, dest)
    return dest
