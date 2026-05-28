#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ysqlsh_bin="${script_dir}/bin/ysqlsh"

if [ ! -f "$ysqlsh_bin" ]; then
  echo "ysqlsh wrapper is not available at $ysqlsh_bin" >&2
  exit 127
fi

echo "Waiting for YugabyteDB primary YSQL..."
until "$ysqlsh_bin" -c "\q" >/dev/null 2>&1; do
  sleep 2
done

exec "$ysqlsh_bin"
