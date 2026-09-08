#!/usr/bin/env python3
"""Assemble ordered source sections during repository-local installation."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKIP = {'.git', '.local', '.local-tool', '.local-tool-app', 'node_modules', 'target'}

for directory, children, _ in os.walk(ROOT):
    children[:] = sorted(name for name in children if name not in SKIP)
    for name in list(children):
        if not name.endswith('.parts'):
            continue
        children.remove(name)
        parts = Path(directory) / name
        target = parts.with_name(name[:-6])
        if not target.resolve().is_relative_to(ROOT):
            raise SystemExit(f'Generated path escapes repository: {target}')
        data = b''.join(part.read_bytes() for part in sorted(parts.glob('*.part')))
        if target.exists() and target.read_bytes() == data:
            continue
        pending = target.with_name('.' + target.name + '.pending')
        pending.write_bytes(data)
        pending.replace(target)
        if data.startswith(b"#!"):
            target.chmod(0o755)
