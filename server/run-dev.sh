#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

ENV_FILE="${ENV_FILE:-.env}"
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "未找到 $ENV_FILE，临时使用 .env.example（建议先 cp .env.example .env）"
  set -a
  source .env.example
  set +a
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ] || \
   ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "17'; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi

echo "JAVA_HOME=${JAVA_HOME}"
echo "启动 MallMini 服务端：profile=${SPRING_PROFILES_ACTIVE:-dev} port=${SERVER_PORT:-8080}"
exec mvn spring-boot:run "$@"
