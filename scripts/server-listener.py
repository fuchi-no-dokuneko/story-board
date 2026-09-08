#!/usr/bin/env python3
"""Locate an IPv4 listening socket owned by the installed server process."""
import pathlib
import socket
import sys

process = pathlib.Path('/proc') / sys.argv[1]
try:
    sockets = {path.readlink().as_posix() for path in (process / 'fd').iterdir()}
    for line in (process / 'net/tcp').read_text().splitlines()[1:]:
        fields = line.split()
        if fields[3] != '0A' or f'socket:[{fields[9]}]' not in sockets:
            continue
        raw_address, raw_port = fields[1].split(':')
        address = socket.inet_ntoa(bytes.fromhex(raw_address)[::-1])
        if address == '0.0.0.0':
            address = '127.0.0.1'
        print(f'https://{address}:{int(raw_port, 16)}')
        sys.exit(0)
except (OSError, ValueError):
    pass
sys.exit(1)
