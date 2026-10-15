import os

import psycopg


DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://reviewer:reviewer@localhost:5433/code_reviewer",
)


def get_connection():
    return psycopg.connect(DATABASE_URL)
