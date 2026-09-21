"""Explicit local maintenance deletion; listing and preview never write."""
import argparse
import sqlite3
import sys
from novel_catalog import ROOT, catalog, connect, display
from delete_rows import delete_rows


def main():
    parser = argparse.ArgumentParser(description='Delete novels / 刪除小說；預設僅預覽')
    parser.add_argument('--database', default=str(ROOT / 'server/data/storyblock.db'))
    selection = parser.add_mutually_exclusive_group()
    selection.add_argument('--novel', action='append', default=[])
    selection.add_argument('--all', action='store_true')
    parser.add_argument('--list', action='store_true')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    if args.apply and (args.list or not (args.novel or args.all)):
        parser.error('--apply needs --novel or --all; incompatible with --list')
    with connect(args.database) as connection:
        preview = catalog(connection, args.novel)
    display(preview)
    if not args.apply:
        print('Preview only / 僅預覽；使用 --apply 才會刪除')
        return
    with connect(args.database, write=True) as connection:
        connection.execute('BEGIN IMMEDIATE')
        if catalog(connection, [row['novel_id'] for row in preview]) != preview:
            raise ValueError('Novels changed since preview; retry / 預覽後小說已變更，請重試')
        delete_rows(connection, [row['novel_id'] for row in preview])
    print(f'Deleted / 已刪除: {len(preview)}')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, sqlite3.Error) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
