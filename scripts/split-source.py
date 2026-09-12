#!/usr/bin/env python3
"""Save an edited assembled source as repository-local sections under 3 KB."""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
for argument in sys.argv[1:]:
    target = Path(argument).resolve()
    if not target.is_relative_to(ROOT) or not target.is_file():
        raise SystemExit('Source must be a file inside this repository')
    directory = target.with_name(target.name + '.parts')
    directory.mkdir(exist_ok=True)
    chunks, current = [], b''
    for line in target.read_bytes().splitlines(keepends=True):
        while len(line) > 2800:
            if current:
                chunks.append(current)
                current = b''
            end = 2800
            while line[end] & 0xc0 == 0x80:
                end -= 1
            chunks.append(line[:end])
            line = line[end:]
        if len(current) + len(line) > 2800:
            chunks.append(current)
            current = b''
        current += line
    if current:
        chunks.append(current)
    for old in directory.glob('*.part'):
        old.unlink()
    for index, data in enumerate(chunks, 1):
        (directory / f'{index:03}.part').write_bytes(data)
