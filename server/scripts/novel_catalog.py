"""Repository-local maintenance reads; no server startup or migration."""
import json
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def connect(database, write=False):
    path = Path(database).resolve(strict=True)
    connection = sqlite3.connect(path.as_uri() + ('?mode=rw' if write else '?mode=ro'), uri=True)
    connection.execute('PRAGMA foreign_keys=ON')
    connection.execute('PRAGMA busy_timeout=5000')
    return connection


def catalog(connection, selected):
    rows = []
    for novel, revision, payload in connection.execute('''
            SELECT n.novel_id, n.head_revision_id, r.canonical_json
            FROM novels n JOIN revisions r ON r.revision_id=n.head_revision_id
            ORDER BY n.novel_id'''):
        document = json.loads(payload)
        rows.append({'novel_id': novel, 'revision_id': revision,
                     'title': document.get('extensions', {}).get('title', 'Untitled / 未命名')})
    if selected:
        missing = set(selected) - {row['novel_id'] for row in rows}
        if missing:
            raise ValueError('Unknown novels / 小說不存在: ' + ', '.join(sorted(missing)))
        rows = [row for row in rows if row['novel_id'] in selected]
    return rows


def display(rows):
    print(json.dumps(rows, ensure_ascii=False, indent=2), flush=True)
