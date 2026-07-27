#!/usr/bin/env bash

# Deploy one of the maintained DigiLedger stacks. Run through deploy/v1/deploy.sh
# or deploy/v2/deploy.sh instead of invoking this file directly.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <v1|v2> [--force]"
  exit 2
fi

VERSION="$1"
shift
FORCE=false
if [[ "${1:-}" == "--force" ]]; then
  FORCE=true
elif [[ $# -gt 0 ]]; then
  echo "Unknown option: $1"
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy/$VERSION"
CONFIG_FILE="$DEPLOY_DIR/config.env"
CONFIG_TEMPLATE="$DEPLOY_DIR/config.env.template"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"
COMPOSE_TEMPLATE="$DEPLOY_DIR/docker-compose.template.yml"

if [[ ! -f "$CONFIG_TEMPLATE" || ! -f "$COMPOSE_TEMPLATE" ]]; then
  echo "Deployment templates for $VERSION were not found."
  exit 1
fi

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required."
  exit 1
fi

ensure_local_files() {
  if [[ ! -f "$CONFIG_FILE" ]]; then
    cp "$CONFIG_TEMPLATE" "$CONFIG_FILE"
    echo "Created $CONFIG_FILE. Review its passwords and public URLs before exposing the service."
  fi
  if [[ ! -f "$COMPOSE_FILE" ]]; then
    cp "$COMPOSE_TEMPLATE" "$COMPOSE_FILE"
    echo "Created $COMPOSE_FILE from its committed template."
  elif ! cmp -s "$COMPOSE_TEMPLATE" "$COMPOSE_FILE"; then
    cp "$COMPOSE_TEMPLATE" "$COMPOSE_FILE"
    echo "Refreshed $COMPOSE_FILE from its committed template."
  fi
}

deploy() {
  ensure_local_files
  docker compose --env-file "$CONFIG_FILE" -f "$COMPOSE_FILE" up --build --detach --remove-orphans
  echo "Deployment complete. Open the URL configured by APP_PORT in $CONFIG_FILE."
}

cd "$ROOT_DIR"
if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Tracked local changes are present; commit or stash them before running deployment."
  exit 1
fi

BEFORE="$(git rev-parse HEAD)"
echo "Updating source code..."
git pull --ff-only
AFTER="$(git rev-parse HEAD)"

if [[ "$FORCE" == true ]]; then
  deploy
elif [[ "$BEFORE" != "$AFTER" ]]; then
  echo "Source code was updated ($(git rev-list --count "$BEFORE..$AFTER") commit(s))."
  read -r -p "Redeploy now? [Y/n] " REPLY
  if [[ ! "$REPLY" =~ ^[Nn]$ ]]; then
    deploy
  else
    echo "Redeployment skipped. Run $0 $VERSION --force when ready."
  fi
else
  echo "Already up to date; no redeployment is needed. Use --force to rebuild anyway."
fi
