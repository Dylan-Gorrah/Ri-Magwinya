#!/usr/bin/env bash
# Rebuilds a throwaway database, applies every migration, runs the SQL tests.
set -e
PG="/c/Program Files/PostgreSQL/18/bin"
SP="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$SP/../.." && pwd)"
P() { PGCONNECT_TIMEOUT=5 "$PG/psql.exe" -w -h 127.0.0.1 -p 55432 -U postgres "$@"; }

P -d postgres -qc "drop database if exists rmtest;" -c "create database rmtest;" >/dev/null 2>&1
P -d rmtest -q -v ON_ERROR_STOP=1 -f "$(dirname "$0")/shim.sql" >/dev/null 2>&1
for f in "$REPO"/supabase/migrations/*.sql; do
  if ! P -d rmtest -q -v ON_ERROR_STOP=1 -f "$f" >/dev/null 2>"$(dirname "$0")/err.txt"; then
    echo "MIGRATION FAILED: $(basename "$f")"; cat "$(dirname "$0")/err.txt"; exit 1
  fi
done
echo "migrations: all $(ls "$REPO"/supabase/migrations/*.sql | wc -l) applied"
echo
P -d rmtest -tA -f "$(dirname "$0")/pricing_test.sql" 2>&1 | grep -E "PASS|FAIL" | sed 's/^NOTICE:  //'
echo
P -d rmtest -tA -f "$(dirname "$0")/rules_test.sql" 2>&1 \
  | grep -E "PASS|FAIL|^---|ERROR" | sed 's/^.*NOTICE:  //'
