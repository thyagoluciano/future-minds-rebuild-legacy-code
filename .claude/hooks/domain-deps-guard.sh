#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Bloqueia dependências de infra no pom.xml do módulo domain/
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só o pom.xml do módulo domain (não o parent POM)
[[ "$FILE_PATH" != */domain/pom.xml ]] && exit 0

# Dependências proibidas dentro de <dependency> (ignora comentários)
FORBIDDEN=$(grep -nEi '<artifactId>(spring[^<]*|kafka[^<]*|hibernate[^<]*|jakarta\.persistence[^<]*|r2dbc[^<]*|h2|postgresql[^<]*)</artifactId>' "$FILE_PATH" 2>/dev/null | grep -v "<!--")

if [[ -n "$FORBIDDEN" ]]; then
  echo "BLOQUEADO: domain/pom.xml não pode ter dependências de infraestrutura!" >&2
  echo "$FORBIDDEN" >&2
  echo "domain/ permite apenas: Kotlin stdlib + Jackson annotations" >&2
  exit 2
fi
exit 0
