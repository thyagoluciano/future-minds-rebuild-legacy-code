#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Roda ktlint standalone (NÃO via Maven) no arquivo editado
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só arquivos Kotlin
[[ "$FILE_PATH" != *.kt ]] && [[ "$FILE_PATH" != *.kts ]] && exit 0

# Verificar se ktlint está instalado
if ! command -v ktlint &>/dev/null; then
  echo "⚠️  ktlint não encontrado. Instale com: brew install ktlint" >&2
  exit 0
fi

# ktlint standalone no arquivo (~1-3s, aceitável)
RESULT=$(ktlint "$FILE_PATH" 2>&1)
if [[ $? -ne 0 ]]; then
  echo "⚠️  ktlint encontrou problemas em $(basename "$FILE_PATH"):" >&2
  echo "$RESULT" >&2
fi
exit 0
