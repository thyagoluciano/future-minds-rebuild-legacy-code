#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Bloqueia imports de infraestrutura no módulo domain/
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só arquivos dentro de domain/src/main
[[ "$FILE_PATH" != */domain/src/main/*.kt ]] && exit 0

# Imports proibidos
FORBIDDEN=$(grep -nE '^import (org\.springframework|javax\.|jakarta\.|org\.apache\.kafka|java\.sql|org\.hibernate|io\.r2dbc)\.' "$FILE_PATH" 2>/dev/null)

if [[ -n "$FORBIDDEN" ]]; then
  echo "BLOQUEADO: domain/ não pode ter dependências de infraestrutura!" >&2
  echo "$FORBIDDEN" >&2
  echo "domain/ permite apenas: Kotlin stdlib + Jackson annotations" >&2
  exit 2
fi
exit 0
