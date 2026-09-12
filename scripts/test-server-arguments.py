#!/usr/bin/env python3
"""Check launcher diagnostics before it touches runtime state."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parent.parent
scratch = root / '.local' / 'tmp'
scratch.mkdir(parents=True, exist_ok=True)
environment = dict(os.environ)
for name in ('STORYBLOCK_PORT_POLICY', 'STORYBLOCK_LOCAL_PORT'):
    environment.pop(name, None)

invalid = [
    ['unknown'], ['start', '--unknown'], ['run', 'public'],
    ['start', '--policy'], ['run', '--port'],
    ['start', '--policy='], ['start', '--port='],
    ['start', '--policy', ''], ['start', '--port', ''],
    ['start', '--policy=internet'], ['run', '--policy', '--port'],
    ['start', '--port=0'], ['start', '--port=65536'],
    ['start', '--port=-1'], ['start', '--port=1.5'],
    ['start', '--port=999999999999999999999999'],
    ['start', '--port=08'], ['start', '--port=$(touch injected)'],
    ['stop', '--policy=public'], ['status', '--port=8443'],
    ['logs', 'extra'],
]
with tempfile.TemporaryDirectory(prefix='server-arguments-', dir=scratch) as directory:
    fixture = Path(directory)
    (fixture / 'scripts').mkdir()
    for name in ('local-server.sh', 'server-arguments.sh'):
        shutil.copy2(root / 'scripts' / name, fixture / 'scripts' / name)
    launcher = fixture / 'scripts' / 'local-server.sh'
    for arguments in invalid:
        result = subprocess.run([str(launcher), *arguments], cwd=fixture,
                                env=environment, capture_output=True, text=True)
        assert result.returncode == 2, (arguments, result)
        assert 'Usage:' in result.stderr, (arguments, result.stderr)
    for arguments in (['--help'], ['-h'], ['start', '--help'], ['run', '--help']):
        result = subprocess.run([str(launcher), *arguments], cwd=fixture,
                                env=environment, capture_output=True, text=True)
        assert result.returncode == 0, (arguments, result)
        assert '--policy' in result.stdout and '--port' in result.stdout
        assert 'public (0.0.0.0)' in result.stdout
    assert not (fixture / '.local').exists(), 'Parsing created runtime state'
    assert not (fixture / 'injected').exists(), 'An argument executed as shell code'
print(f'PASS: {len(invalid)} invalid commands and 4 help forms, without runtime writes')
