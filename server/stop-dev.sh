#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

PORT="${SERVER_PORT:-8080}"
PIDS="$(lsof -ti:"$PORT" 2>/dev/null || true)"
if [ -z "$PIDS" ]; then
  echo "端口 $PORT 没有运行中的服务"
else
  # shellcheck disable=SC2086
  kill $PIDS
  echo "已停止端口 $PORT"
fi
