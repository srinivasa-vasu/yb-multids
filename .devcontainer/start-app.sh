#!/usr/bin/env bash
set -euo pipefail

if pgrep -f "spring-boot:run" >/dev/null 2>&1; then
  exit 0
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is not available in the devcontainer image." >&2
  exit 127
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ysqlsh_bin="${script_dir}/bin/ysqlsh"

if [ ! -f "$ysqlsh_bin" ]; then
  echo "ysqlsh wrapper is not available at $ysqlsh_bin" >&2
  exit 127
fi

cd /workspaces/yb-multids

echo "Waiting for YugabyteDB primary YSQL..."
until "$ysqlsh_bin" -c "\q" >/dev/null 2>&1; do
  sleep 2
done

echo "Waiting for YugabyteDB read-replica YSQL..."
until YSQL_HOST=yb-rr "$ysqlsh_bin" -c "\q" >/dev/null 2>&1; do
  sleep 2
done

echo "Starting Spring Boot app..."
nohup mvn -q spring-boot:run > /tmp/multids.log 2>&1 &
