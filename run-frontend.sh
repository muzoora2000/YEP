#!/usr/bin/env bash
# run-frontend.sh — start the YEP Next.js frontend locally
# Usage: ./run-frontend.sh
# Requires: Node 20+, pnpm or npm

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/frontend"

# Default backend URL for local development
export BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
export NEXT_PUBLIC_APP_URL="${NEXT_PUBLIC_APP_URL:-http://localhost:3000}"

echo "▶ Starting YEP frontend ..."
echo "  API proxy target: $BACKEND_URL"
echo "  App URL: $NEXT_PUBLIC_APP_URL"

if command -v pnpm &>/dev/null; then
  pnpm dev
elif command -v npm &>/dev/null; then
  npm run dev
else
  echo "ERROR: Neither pnpm nor npm found in PATH." >&2
  exit 1
fi
