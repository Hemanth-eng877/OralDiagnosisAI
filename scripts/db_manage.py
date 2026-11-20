"""Database Management and SQLite Migration Utility

Provides schema initialization, seeding of synthetic clinical records,
and database integrity verification.

Usage:
    python scripts/db_manage.py init [--db-path database.db]
    python scripts/db_manage.py seed [--db-path database.db]
    python scripts/db_manage.py verify [--db-path database.db]
"""

import argparse
import os
import sqlite3
import sys
try:
    from werkzeug.security import generate_password_hash
except ImportError:
    import hashlib
    def generate_password_hash(password):
        return "scrypt$" + hashlib.sha256(password.encode()).hexdigest()

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SCHEMA_FILE = os.path.join(BASE_DIR, "schema.sql")
DEFAULT_DB = os.path.join(BASE_DIR, "database.db")


def get_connection(db_path=DEFAULT_DB):
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db(db_path=DEFAULT_DB):
    print(f"Initializing database at: {db_path}")
    if not os.path.exists(SCHEMA_FILE):
        print(f"Error: schema.sql not found at '{SCHEMA_FILE}'")
        sys.exit(1)

    with open(SCHEMA_FILE, "r", encoding="utf-8") as f:
        schema_sql = f.read()

    conn = get_connection(db_path)
    with conn:
        conn.executescript(schema_sql)
    conn.close()
    print("Database tables and indexes created successfully.")


def seed_test_data(db_path=DEFAULT_DB):
    init_db(db_path)
    print(f"Seeding synthetic clinical records into: {db_path}")
    conn = get_connection(db_path)
    with conn:
        # 1. Seed demo doctor user
        cur = conn.execute(
            """
            INSERT OR IGNORE INTO users (name, email, password_hash)
            VALUES (?, ?, ?)
            """,
            ("Dr. Sarah Mitchell", "demo.doctor@gmail.com", generate_password_hash("DemoDoc2026!")),
        )
        cur = conn.execute("SELECT id FROM users WHERE email = ?", ("demo.doctor@gmail.com",))
        user_row = cur.fetchone()
        user_id = user_row["id"] if user_row else 1

        # 2. Seed demo patients
        patients_data = [
            (user_id, "Arthur Pendelton", 62, "Male", "+1-555-0192", "arthur.p@example.com", "Chronic smoker, white leukoplakia on left buccal mucosa."),
            (user_id, "Elena Rostova", 45, "Female", "+1-555-0143", "elena.r@example.com", "Non-smoker, mild erythema on ventral tongue surface."),
            (user_id, "Marcus Vance", 58, "Male", "+1-555-0187", "marcus.v@example.com", "Betel nut user, ulcerated lateral border lesion."),
            (user_id, "Clara Oswald", 34, "Female", "+1-555-0112", "clara.o@example.com", "Routine dental screening, normal mucosal appearance."),
        ]
        conn.executemany(
            """
            INSERT INTO patients (user_id, full_name, age, gender, phone, email, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            patients_data,
        )

        # 3. Seed demo diagnoses
        cur = conn.execute("SELECT id FROM patients WHERE user_id = ?", (user_id,))
        patient_ids = [row["id"] for row in cur.fetchall()]

        if len(patient_ids) >= 4:
            diagnoses_data = [
                (user_id, patient_ids[0], "demo_sample_1.png", "OSCC", 96.8),
                (user_id, patient_ids[1], "demo_sample_2.png", "Normal", 94.2),
                (user_id, patient_ids[2], "demo_sample_3.png", "OSCC", 98.4),
                (user_id, patient_ids[3], "demo_sample_4.png", "Normal", 99.1),
            ]
            conn.executemany(
                """
                INSERT INTO diagnoses (user_id, patient_id, image_filename, diagnosis, confidence)
                VALUES (?, ?, ?, ?, ?)
                """,
                diagnoses_data,
            )

    conn.close()
    print("Database seeding completed with demo doctor, 4 patients, and 4 diagnosis records.")


def verify_integrity(db_path=DEFAULT_DB):
    print(f"Verifying database integrity at: {db_path}")
    if not os.path.exists(db_path):
        print(f"Error: Database file does not exist: {db_path}")
        return False

    conn = get_connection(db_path)
    cur = conn.execute("PRAGMA integrity_check")
    result = cur.fetchone()[0]
    print(f"PRAGMA integrity_check: {result}")

    tables = ["users", "patients", "diagnoses"]
    for table in tables:
        count = conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        print(f"  Table '{table}': {count} records")

    conn.close()
    return result == "ok"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="SQLite Database Management Tool")
    parser.add_argument("action", choices=["init", "seed", "verify"], help="Action to execute")
    parser.add_argument("--db-path", default=DEFAULT_DB, help="Path to SQLite database")
    args = parser.parse_args()

    if args.action == "init":
        init_db(args.db_path)
    elif args.action == "seed":
        seed_test_data(args.db_path)
    elif args.action == "verify":
        verify_integrity(args.db_path)
