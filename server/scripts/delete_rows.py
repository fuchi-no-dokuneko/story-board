"""Delete selected ownership trees atomically, restoring immutable triggers."""


def delete_rows(connection, novels):
    if not novels:
        return
    connection.execute('PRAGMA defer_foreign_keys=ON')
    connection.execute('CREATE TEMP TABLE selected_novels (id TEXT PRIMARY KEY)')
    connection.executemany('INSERT INTO selected_novels VALUES (?)', ((n,) for n in novels))
    selected = 'SELECT id FROM selected_novels'
    profiles = f'SELECT profile_id FROM style_profiles WHERE novel_id IN ({selected})'
    jobs = f'SELECT analysis_id FROM analysis_jobs WHERE novel_id IN ({selected})'
    runs = f'SELECT run_id FROM monitor_runs WHERE novel_id IN ({selected})'
    proposals = f'SELECT proposal_id FROM rewrite_candidate_reservations WHERE novel_id IN ({selected})'
    triggers = connection.execute("SELECT name, sql FROM sqlite_master WHERE type='trigger'").fetchall()
    # DDL is transactional: other connections never see the temporary suspension.
    for name, _ in triggers:
        connection.execute('DROP TRIGGER "' + name.replace('"', '""') + '"')
    children = {
        'analysis_artifacts': ('analysis_id', jobs),
        'analysis_window_findings': ('analysis_id', jobs),
        'analysis_runs': ('analysis_id', jobs),
        'monitor_issues': ('run_id', runs),
        'monitor_proposed_operations': ('run_id', runs),
        'rewrite_reserved_findings': ('proposal_id', proposals),
        'style_profile_lifecycle_events': ('profile_id', profiles),
        'style_profile_versions': ('profile_id', profiles),
    }
    for table, (column, query) in children.items():
        connection.execute(f'DELETE FROM {table} WHERE {column} IN ({query})')
    connection.execute(f'''DELETE FROM short_identifiers WHERE owner IN ({selected})
        OR owner IN (SELECT identifier FROM short_identifiers WHERE owner IN ({selected}))''')
    tables = connection.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
    for (table,) in tables:
        quoted = '"' + table.replace('"', '""') + '"'
        columns = {row[1] for row in connection.execute(f'PRAGMA table_info({quoted})')}
        if 'novel_id' in columns:
            connection.execute(f'DELETE FROM {quoted} WHERE novel_id IN ({selected})')
    for _, sql in triggers:
        connection.execute(sql)
    if connection.execute('PRAGMA foreign_key_check').fetchone():
        raise ValueError('Deletion has references from retained data / 保留資料仍有引用，已取消刪除')
    connection.execute('DROP TABLE selected_novels')
