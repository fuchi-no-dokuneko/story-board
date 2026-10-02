#!/usr/bin/env python3
"""Package both browser applications through the existing public asset routes."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STATIC = ROOT / 'server/src/main/resources/static'
SOURCE = ROOT / 'client-web-v2/src'


def original(name):
    parts = STATIC / (name + '.parts')
    if parts.is_dir():
        return ''.join(p.read_text() for p in sorted(parts.glob('*.part')))
    return (STATIC / name).read_text()


def write(name, value):
    (STATIC / name).write_text(value)


legacy = '\n'.join(original(name) for name in [
    'app.js', 'reader-actions.js', 'style-score-table.js',
    'style-comparison.js', 'quality-view.js', 'quality-report.js',
])
modern = '\n'.join(p.read_text() for p in sorted((SOURCE / 'js').glob('*.js')))
write('app.js', "if (new URLSearchParams(location.search || '').get('ui') === 'v2') {\n"
      + '(() => {\n\'use strict\';\n' + modern + '\n})();\n} else {\n'
      + legacy + '\n}\n')
write('styles.css', original('styles.css') + '\n'
      + '\n'.join(p.read_text() for p in sorted((SOURCE / 'css').glob('*.css'))))
