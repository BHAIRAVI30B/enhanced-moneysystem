import mysql.connector  # type: ignore[import]
import snowflake.connector  # type: ignore[import]

from config import MYSQL_CONFIG, SNOWFLAKE_CONFIG


# Mapping between MySQL tables and Snowflake tables, with columns
TABLES = [
    {
        "snowflake_table": "ACCOUNTS",
        "mysql_table": "account",
        "pk": "ID",
        "columns": [
            "ID",
            "ACCOUNT_ID",
            "BALANCE",
            "HOLDER_NAME",
            "LAST_UPDATED",
            "STATUS",
            "VERSION",
        ],
    },
    {
        "snowflake_table": "TRANSACTION_LOG",
        "mysql_table": "transaction_log",
        "pk": "ID",
        "columns": [
            "ID",
            "AMOUNT",
            "CREATED_ON",
            "FAILURE_REASON",
            "IDEMPOTENCY_KEY",
            "STATUS",
            "FROM_ACCOUNT_ID",
            "TO_ACCOUNT_ID",
        ],
    },
    {
        "snowflake_table": "TRANSACTION_DETAILS",
        "mysql_table": "transaction_details",
        "pk": "ID",
        "columns": [
            "ID",
            "CATEGORY",
            "NOTE",
            "TRANSACTION_LOG_ID",
        ],
    },
    {
        "snowflake_table": "USERS",
        "mysql_table": "users",
        "pk": "ID",
        "columns": [
            "ID",
            "USERNAME",
            "PASSWORD",
            "ACCOUNT_ID",
        ],
    },
    {
        "snowflake_table": "ROLES",
        "mysql_table": "roles",
        "pk": "ID",
        "columns": [
            "ID",
            "ROLE_NAME",
        ],
    },
    {
        "snowflake_table": "USER_ROLES",
        "mysql_table": "user_roles",
        "pk": None,  # no single numeric PK; we reload fully each run
        "columns": [
            "USER_ID",
            "ROLE_ID",
        ],
    },
]


def get_mysql_connection():
    return mysql.connector.connect(
        host=MYSQL_CONFIG["host"],
        port=MYSQL_CONFIG["port"],
        database=MYSQL_CONFIG["database"],
        user=MYSQL_CONFIG["user"],
        password=MYSQL_CONFIG["password"],
    )


def get_snowflake_connection():
    return snowflake.connector.connect(
        account=SNOWFLAKE_CONFIG["account"],
        user=SNOWFLAKE_CONFIG["user"],
        password=SNOWFLAKE_CONFIG["password"],
        warehouse=SNOWFLAKE_CONFIG["warehouse"],
        database=SNOWFLAKE_CONFIG["database"],
        schema=SNOWFLAKE_CONFIG["schema"],
    )


def ensure_sync_control(sf_cursor):
    """
    Creates a SYNC_CONTROL table in Snowflake to track the last synced ID
    for each table we replicate.
    """
    sf_cursor.execute(
        """
        CREATE TABLE IF NOT EXISTS SYNC_CONTROL (
            TABLE_NAME STRING PRIMARY KEY,
            LAST_ID    NUMBER(38,0)
        )
        """
    )


def get_last_id(sf_cursor, snowflake_table):
    sf_cursor.execute(
        "SELECT LAST_ID FROM SYNC_CONTROL WHERE TABLE_NAME = %s",
        (snowflake_table,),
    )
    row = sf_cursor.fetchone()
    if row is None:
        sf_cursor.execute(
            "INSERT INTO SYNC_CONTROL (TABLE_NAME, LAST_ID) VALUES (%s, %s)",
            (snowflake_table, 0),
        )
        return 0
    return int(row[0] or 0)


def update_last_id(sf_cursor, snowflake_table, last_id):
    sf_cursor.execute(
        "UPDATE SYNC_CONTROL SET LAST_ID = %s WHERE TABLE_NAME = %s",
        (last_id, snowflake_table),
    )


def fetch_new_rows(mysql_cursor, mysql_table, pk_column, columns, last_id):
    col_list = ", ".join(columns)

    if pk_column is None:
        # For tables without a numeric PK (e.g., USER_ROLES), do full load.
        query = f"SELECT {col_list} FROM {mysql_table}"
        mysql_cursor.execute(query)
        return mysql_cursor.fetchall()

    query = f"""
        SELECT {col_list}
        FROM {mysql_table}
        WHERE {pk_column} > %s
        ORDER BY {pk_column}
    """
    mysql_cursor.execute(query, (last_id,))
    return mysql_cursor.fetchall()


def insert_rows_into_snowflake(sf_cursor, snowflake_table, columns, rows):
    if not rows:
        return

    col_list = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))

    insert_sql = f"""
        INSERT INTO {snowflake_table} ({col_list})
        VALUES ({placeholders})
    """
    sf_cursor.executemany(insert_sql, rows)


def sync_table(mysql_cursor, sf_cursor, table_cfg):
    snowflake_table = table_cfg["snowflake_table"]
    mysql_table = table_cfg["mysql_table"]
    pk = table_cfg["pk"]
    columns = table_cfg["columns"]

    print(f"--- Syncing {mysql_table} -> {snowflake_table} ---")

    if pk is None:
        # For small lookup tables without a numeric PK: truncate + reload
        sf_cursor.execute(f"TRUNCATE TABLE {snowflake_table}")
        rows = fetch_new_rows(mysql_cursor, mysql_table, None, columns, 0)
        if not rows:
            print(f"No rows found for {mysql_table}.")
            return

        insert_rows_into_snowflake(sf_cursor, snowflake_table, columns, rows)
        print(f"Reloaded {len(rows)} rows into {snowflake_table}.")
        return

    last_id = get_last_id(sf_cursor, snowflake_table)
    print(f"Last synced ID for {snowflake_table}: {last_id}")

    rows = fetch_new_rows(mysql_cursor, mysql_table, pk, columns, last_id)
    if not rows:
        print(f"No new rows for {snowflake_table}.")
        return

    print(f"Found {len(rows)} new rows for {snowflake_table}.")
    insert_rows_into_snowflake(sf_cursor, snowflake_table, columns, rows)

    pk_index = columns.index(pk)
    max_id = max(int(row[pk_index]) for row in rows)
    update_last_id(sf_cursor, snowflake_table, max_id)
    print(f"Synced {snowflake_table} up to ID = {max_id}")


def main():
    mysql_conn = get_mysql_connection()
    sf_conn = get_snowflake_connection()

    try:
        mysql_cursor = mysql_conn.cursor()
        sf_cursor = sf_conn.cursor()

        ensure_sync_control(sf_cursor)

        for table_cfg in TABLES:
            sync_table(mysql_cursor, sf_cursor, table_cfg)

        sf_conn.commit()
        mysql_conn.commit()
        print("All tables synced successfully.")

    finally:
        try:
            mysql_conn.close()
        except Exception:
            pass
        try:
            sf_conn.close()
        except Exception:
            pass


if __name__ == "__main__":
    main()

