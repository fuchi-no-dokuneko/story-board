"""Exercise retention using complete archived snapshots and their sidecars."""
import json
import shutil
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

source, directory = map(Path, sys.argv[1:])
directory.mkdir()
original = json.loads(Path(str(source) + '.json').read_text())
archives = []
start = datetime.now(timezone.utc) - timedelta(days=200)
for index in range(49):
    created = start - timedelta(seconds=index)
    name = f'storyblock-{created:%Y%m%dT%H%M%SZ}.db.zst'
    backup = directory / name
    shutil.copyfile(source, backup)
    manifest = dict(original, artifact=name, created_at=created.strftime('%Y-%m-%dT%H:%M:%SZ'))
    Path(str(backup) + '.json').write_text(json.dumps(manifest))
    Path(str(backup) + '.sha256').write_text(f'{manifest["sha256"]}  {name}\n')
    archives.append(backup)

oldest = min(archives)
preview = subprocess.check_output(['scripts/prune-backups.sh', str(directory)], text=True)
assert preview.splitlines() == [oldest.name]
assert oldest.exists()
subprocess.run(['scripts/prune-backups.sh', str(directory), '--apply'], check=True,
               stdout=subprocess.DEVNULL)
assert len(list(directory.glob('*.db.zst'))) == 48
for suffix in ('', '.json', '.sha256'):
    assert not Path(str(oldest) + suffix).exists()
