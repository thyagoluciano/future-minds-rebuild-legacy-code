#!/bin/bash
# PreToolUse matcher "Bash"
# Bloqueia git commit direto na main/master
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Sair imediatamente se não for git commit
[[ "$COMMAND" != *"git commit"* ]] && exit 0
[[ "$COMMAND" == *"--amend"* ]] && exit 0

BRANCH=$(git branch --show-current 2>/dev/null)
if [[ "$BRANCH" == "main" || "$BRANCH" == "master" ]]; then
  echo "BLOQUEADO: commit direto na branch '$BRANCH' não permitido." >&2
  echo "Crie uma feature branch: git checkout -b feat/tXX-descricao" >&2
  exit 2
fi
exit 0
