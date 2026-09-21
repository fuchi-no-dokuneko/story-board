"""Batch export through the project's existing Java renderer."""
import argparse
import subprocess
import sys
from pathlib import Path
from novel_catalog import ROOT


def main():
    parser = argparse.ArgumentParser(description='Batch HTML/PDF export / 批次匯出')
    parser.add_argument('--database', default=str(ROOT / 'server/data/storyblock.db'))
    parser.add_argument('--output', default=str(ROOT / 'fan-out/novels'))
    parser.add_argument('--format', choices=['html', 'pdf', 'both'], default='both')
    select = parser.add_mutually_exclusive_group(required=True)
    select.add_argument('--all', action='store_true')
    select.add_argument('--novel', action='append')
    args = parser.parse_args()
    output = Path(args.output).resolve()
    if not output.is_relative_to(ROOT):
        parser.error('Output must remain in this repository / 匯出位置須在儲存庫內')
    database = Path(args.database).resolve(strict=True)
    command = [str(ROOT / 'client-cli/run.sh'), 'export-books', str(database),
               str(output), args.format, *(args.novel or [])]
    return subprocess.call(command, cwd=ROOT)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except OSError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
