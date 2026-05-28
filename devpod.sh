#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v devpod >/dev/null 2>&1; then
  echo "devpod is not installed or not on PATH." >&2
  echo "Install it from https://devpod.sh/docs/getting-started/install" >&2
  exit 1
fi

exec devpod up "$repo_root" --ide vscode "$@"
