#!/usr/bin/env python3
"""Package the ZIP designs and legacy app through existing asset URLs."""
import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STATIC = ROOT / 'server/src/main/resources/static'
SOURCE = ROOT / 'client-web-v2/src'
scope = runpy.run_path(str(Path(__file__).with_name('scope_css.py')))['scope']


def read(path):
    parts = path if path.is_dir() else Path(str(path) + '.parts')
    return (''.join(p.read_text() for p in sorted(parts.glob('*.part')))
            if parts.is_dir() else path.read_text())


def write(name, value):
    (STATIC / name).write_text(value)


legacy = '\n'.join(read(STATIC / name) for name in [
    'app.js', 'reader-actions.js', 'style-score-table.js',
    'style-comparison.js', 'quality-view.js', 'quality-report.js',
])
modern = '\n'.join(p.read_text() for p in sorted((SOURCE / 'js').glob('*.js')))
design = SOURCE / 'design'
modern += '\nconst desktopShell=' + json.dumps(read(design / 'desktop.html')) + ';\n'
for mode in ['desktop', 'mobile']:
    modern += 'const ' + mode + 'Words=' + read(design / (mode + '-words.js')) + ';\n'
for directory in ['shared', 'desktop', 'mobile', 'runtime']:
    for source in sorted((SOURCE / directory).iterdir()):
        if source.name.endswith('.js.parts') or (source.suffix == '.js'
                and not Path(str(source) + '.parts').is_dir()):
            modern += '\n' + read(source)
write('app.js', "if (new URLSearchParams(location.search || '').get('ui') === 'v2') {\n"
      + '(() => {\n\'use strict\';\n' + modern + '\n})();\n} else {\n'
      + legacy + '\n}\n')
styles = scope(read(STATIC / 'styles.css'), 'html:not([data-ui="v2"])')
for mode in ['desktop', 'mobile']:
    prefix = f'html[data-ui="v2"][data-layout="{mode}"]'
    styles += '\n' + scope(read(design / (mode + '.css')), prefix)
    overrides = SOURCE / mode / 'integration.css'
    if overrides.exists():
        styles += '\n' + scope(overrides.read_text(), prefix)
write('styles.css', styles + '\n')
