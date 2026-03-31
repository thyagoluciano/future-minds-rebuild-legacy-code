# Rebuild do Sales Manager — Guia para Quem Não Conhece o Módulo

## Context

Você é um desenvolvedor que acabou de receber a missão: modernizar o Sales Manager, um sistema legado de 2017. Você nunca viu esse código antes. Não sabe o que ele faz, quantos módulos tem, nem que tecnologias usa. Tudo que você sabe é que precisa reconstruí-lo com uma stack moderna mantendo compatibilidade total.

Este guia usa o Claude Code como parceiro de trabalho desde o primeiro momento — para entender, especificar, planejar e executar.

---

## PARTE 1: DESCOBERTA — Entendendo o que você precisa reconstruir

> Objetivo: sair do zero absoluto e chegar a um entendimento completo do sistema.
> Ferramentas: apenas Claude Code + legado. Nenhuma configuração ainda.

### Passo 1.1 — Primeira conversa: "O que é isso?"

Abra o Claude Code na pasta do projeto legado e pergunte:

```
Analise este projeto inteiro. Quero entender:
1. O que este sistema faz? Qual problema de negócio resolve?
2. Quem são os usuários/clientes?
3. Qual é o fluxo principal (happy path)?
4. Quais são os conceitos de domínio centrais?
```

**O que você vai descobrir**: O Sales Manager é um "carrinho de compras" para telecom. Ele coleta informações de uma compra (cliente, itens do catálogo, pagamento, cupons, frete, etc.), valida tudo e depois envia para o Customer Order Manager (COM) processar. Usa CQRS + Event Sourcing.

### Passo 1.2 — Mapear a arquitetura

```
Mapeie a arquitetura deste projeto:
1. Quantos módulos existem e qual a responsabilidade de cada um?
2. Como eles se relacionam (dependências)?
3. Existe separação command/query? Como funciona?
4. Quais são os serviços externos com que ele se integra?
5. Desenhe o fluxo de dados de uma compra do início ao fim.
```

**O que você vai descobrir**: 12 módulos Maven, 3 aplicações Spring Boot separadas (command, query, consumer), integração com 6+ serviços externos via Feign.

### Passo 1.3 — Identificar o que precisa ser substituído

```
Liste todas as dependências proprietárias (br.com.zup.*) no pom.xml.
Para cada uma, explique:
1. O que ela faz
2. Por que precisa ser substituída
3. Qual seria o substituto moderno em Spring Boot 3.x
```

**O que você vai descobrir**: 17 bibliotecas Zup abandonadas que precisam de substituição — desde o framework de event sourcing até clientes HTTP, serialização, multi-tenancy e autenticação.

### Passo 1.4 — Mapear contratos (a parte mais crítica)

```
Extraia TODOS os contratos que devem permanecer idênticos no rebuild:

1. ENDPOINTS: para cada endpoint REST, extraia:
   - Método HTTP + path
   - Headers obrigatórios
   - Request body (todos os campos, tipos, obrigatoriedade)
   - Response body (todos os campos, tipos)
   - Status codes possíveis

2. EVENTOS: para cada evento de domínio, extraia:
   - Nome da classe
   - Todos os campos com tipos
   - Formato JSON serializado

3. KAFKA: tópicos, formato de mensagem, consumer groups

4. SEGURANÇA: roles, headers de multi-tenancy, filtros

Salve tudo em docs/api-contracts/ e docs/events/ como referência.
```

**Por que isso é crítico**: O requisito #1 do rebuild é compatibilidade total. Se um campo JSON mudar de nome ou um status code mudar, o sistema novo não substitui o antigo sem downtime.

### Passo 1.5 — Entender o aggregate PurchaseOrder

```
Analise o PurchaseOrder aggregate em detalhe:
1. Quais são os estados possíveis (status) e as transições?
2. Quais são TODOS os eventos de domínio? (liste nome + campos)
3. Quais são TODOS os comandos? (liste nome + campos)
4. Quais são os command handlers e o que cada um faz?
5. Quais validações existem antes de cada transição?
6. Como funciona o checkout? (é o fluxo mais complexo)
```

**O que você vai descobrir**: 4 status (OPENED→CHECKED_OUT→COMPLETED/FAILED/CANCELED), 24 eventos, 24 comandos, checkout com validação externa e chamada ao COM.

---

## PARTE 2: ESPECIFICAÇÃO — Documentando o que vai ser construído

> Objetivo: criar um documento de especificação que servirá de base para TUDO.
> Este documento é seu "north star" — toda task e toda decisão técnica derivam dele.

### Passo 2.1 — Gerar a Especificação Técnica

Com todo o conhecimento da Parte 1, peça ao Claude:

```
Com base em toda a análise do legado, crie uma ESPECIFICAÇÃO TÉCNICA
para o rebuild em docs/SPEC.md. Estruture assim:

## 1. Visão Geral
- O que o sistema faz (1 parágrafo para negócio)
- Requisito de compatibilidade

## 2. Decisões Arquiteturais
- Stack alvo (Kotlin 2.x, Spring Boot 3.4, JDK 21, etc.)
- Consolidação de módulos: de 12 legados → 7 novos (explicar por quê)
- Event Store: PostgreSQL com JSONB (substituindo lib Zup)
- Migrations: Flyway (substituindo Liquibase)
- HTTP Clients: Spring Cloud OpenFeign (substituindo SDK Zup)
- Auth: Spring Security 6 + OAuth2 (substituindo Zup IAM)
- Testes: JUnit 5 + MockK + Testcontainers

## 3. Mapeamento de Módulos (legado → novo)
Tabela mostrando de onde vem cada funcionalidade

## 4. Contratos de API (referência ao docs/api-contracts/)
## 5. Eventos de Domínio (referência ao docs/events/)
## 6. Integrações Externas (cada Feign client com endpoints)
## 7. Segurança e Multi-tenancy
## 8. Riscos e Mitigações
```

### Passo 2.2 — Revisar a especificação

Leia o documento gerado e valide:
- A consolidação de módulos faz sentido?
- Algum requisito de negócio ficou de fora?
- As decisões técnicas estão adequadas?

Se necessário, discuta com o Claude:
```
Na spec, você consolidou command-repository + events + producer em
sm-infrastructure. Isso não vai gerar um módulo grande demais?
Quais são os trade-offs?
```

---

## PARTE 3: PLANEJAMENTO — Quebrando em tasks

> Objetivo: criar um plano de execução com tasks independentes e paralelizáveis.
> A especificação guia; o plano operacionaliza.

### Passo 3.1 — Definir tasks com base na spec

```
Com base na especificação em docs/SPEC.md, quebre o rebuild em tasks.
Cada task deve:
- Ter um escopo claro e testável
- Ser independente ou ter dependências explícitas
- Caber em uma sessão de trabalho (não tasks de 3 dias)

Organize por camada:
1. Foundation (setup projeto, build, docker)
2. Domain (VOs, eventos, aggregate, commands, handlers, services)
3. Infrastructure (event store, kafka, feign clients)
4. Command App (controllers, handlers, security, filtros)
5. Query App (read model, projeções, endpoints)
6. Consumer App (kafka consumer, callbacks)
7. Cross-cutting (CI/CD, métricas, docs, testes de carga)

Para cada task, defina:
- ID (T01, T02, ...)
- Título
- Critérios de aceitação (checklist)
- Módulo alvo
- Depende de quais tasks
```

### Passo 3.2 — Montar o grafo de dependências e waves

```
Com as tasks definidas, monte:
1. Um grafo de dependências (quais tasks dependem de quais)
2. Waves de execução (tasks na mesma wave são paralelas)

Regras:
- Wave 0 é sempre T01 (foundation) — solo
- Tasks na mesma wave NÃO podem ter dependências entre si
- Na dúvida, coloque em waves diferentes
```

### Passo 3.3 — Criar issues no GitHub

```
Crie issues no GitHub para cada task usando gh issue create.
Use o formato:
- Título: [TXX] Descrição
- Body: Objetivo, Critérios de Aceitação, Depende de
- Labels: wave-N, módulo
```

---

## PARTE 4: CONFIGURAR O CLAUDE CODE

> Objetivo: agora sim, configurar o Claude Code para o trabalho.
> Só configure o que vai usar IMEDIATAMENTE. Itere conforme a necessidade.

### Passo 4.1 — Criar o CLAUDE.md (máximo 200 linhas)

O CLAUDE.md é carregado automaticamente em toda sessão. Ele deve conter APENAS o que o Claude precisa saber para trabalhar corretamente.

```
Crie o CLAUDE.md baseado na especificação. Inclua:
- Overview do projeto (2-3 linhas)
- Requisito de compatibilidade com legado
- Path do legado para referência
- Tech stack (tabela compacta)
- Estrutura de módulos (árvore simples)
- Dependências entre módulos (incluindo regra do sm-domain)
- Comandos de build (os que vou usar todo dia)
- Convenções de código (Kotlin style resumido)
- Formato de commit
- Link para issues
MÁXIMO 200 linhas. Se passar, mova detalhes para .claude/rules/.
```

### Passo 4.2 — Criar settings.json (só permissões essenciais)

**Arquivo**: `.claude/settings.json`

Comece mínimo. Você sempre pode adicionar mais depois.

```json
{
  "permissions": {
    "allow": [
      "Bash(./gradlew *)",
      "Bash(git *)",
      "Bash(gh *)",
      "Bash(pwd)",
      "Bash(mkdir -p *)",
      "Read",
      "Glob",
      "Grep"
    ]
  }
}
```

**Por que tão mínimo?** Porque você é novo no projeto. Quer revisar cada arquivo que o Claude cria/modifica. Conforme ganhar confiança, adicione `Write`, `Edit`, `Skill`.

**NÃO configure hooks ainda.** Hooks são otimização para quando o fluxo já está funcionando. Adicioná-los agora só cria fricção.

### Passo 4.3 — Criar UMA regra contextual (sm-domain)

A única regra que vale criar desde o dia 1 é a do domain, porque é a restrição arquitetural mais crítica do projeto:

**Arquivo**: `.claude/rules/domain.md`
```yaml
---
paths:
  - "sm-domain/**"
---

# Regras do sm-domain — Module Puro

Este módulo NÃO PODE depender de:
- org.springframework (nenhum pacote)
- java.sql / javax.sql
- org.apache.kafka
- javax.persistence / jakarta.persistence

Dependências permitidas: Kotlin stdlib, Jackson annotations.

Padrões:
- @JvmInline value class para IDs tipados
- sealed interface para eventos e comandos
- data class para Value Objects
- Nomes de campos JSON IDÊNTICOS ao legado (usar @JsonProperty se necessário)
```

Demais regras: crie conforme chegar nos módulos correspondentes.

### Passo 4.4 — Inicializar Memory

O memory serve para informações que você descobre durante o trabalho e que serão úteis em sessões futuras (workarounds de ambiente, decisões tomadas, etc.).

**Não preencha previamente com templates vazios.** O memory é construído organicamente:

```
Lembre-se: estamos usando Podman em vez de Docker nesta máquina.
O socket está em /var/folders/.../podman/podman-machine-default-api.sock
```

O Claude salva automaticamente no memory system.

---

## PARTE 5: EXECUÇÃO — Wave 0 (Foundation)

> Wave 0 é sempre manual e individual. É o alicerce.

### Passo 5.1 — Implementar T01: Setup do Projeto

```
Implemente o setup do projeto Gradle multi-módulo baseado na spec.
Consulte o legado para nomes de pacotes e estrutura.
Após implementar, execute ./gradlew build para validar.
```

O Claude vai:
1. Criar `settings.gradle.kts` com os módulos
2. Criar `build.gradle.kts` raiz com plugins e dependências
3. Criar `gradle/libs.versions.toml` (version catalog)
4. Criar `build.gradle.kts` de cada módulo
5. Criar `docker-compose.yml` para infraestrutura
6. Criar `src/` vazio em cada módulo

**Valide**: `./gradlew build` deve compilar sem erros.

### Passo 5.2 — Primeiro commit e ajustes de ambiente

Neste ponto, problemas de ambiente aparecem (SSL, Gradle wrapper, container runtime). Resolva-os e salve no memory:

```
O Gradle wrapper tem problema de SSL. Salvei no memory.
Vou usar Gradle local como workaround.
```

---

## PARTE 6: EXECUÇÃO — Waves 1+ (Iterativo)

> A partir daqui, o ritmo é: implementar → validar → entregar → próxima wave.

### Passo 6.1 — Waves individuais (1-3 tasks)

Para waves pequenas, trabalhe direto com o Claude:

```
Implemente T05: Value Objects e Enums do domínio.
- Consulte o legado em realwave-sales-manager-domain/src/.../domain/
- Cada VO deve ter serialização JSON idêntica ao legado
- Crie testes de serialização round-trip
- Quando terminar, rode ./gradlew :sm-domain:test
```

Após validar, faça o commit:
```
/commit T05
```

### Passo 6.2 — Quando introduzir Skills

**Crie skills quando perceber repetição.** Exemplos:

- Após o 3º commit manual, crie `/commit` para padronizar
- Após rodar `./gradlew test` pela 5ª vez, crie `/test` para encapsular (incluindo variáveis de ambiente do Testcontainers)
- Após fazer o fluxo completo (test→lint→commit→push→PR) 2-3 vezes, crie `/deliver`

**Não crie todas as skills no dia 1.** Crie conforme a necessidade emerge.

**Como criar uma skill** (exemplo `/test`):

```
Crie uma skill /test em .claude/skills/test/SKILL.md que:
1. Receba um módulo opcional via $ARGUMENTS
2. Se módulo especificado: ./gradlew :$ARGUMENTS:test
3. Se vazio: ./gradlew test
4. Configure as variáveis de ambiente do Testcontainers
5. Crie marker /tmp/sm-tests-passed-$(date +%Y%m%d) se passar
```

### Passo 6.3 — Hooks do Claude Code

Hooks são scripts que rodam automaticamente antes (`PreToolUse`) ou depois (`PostToolUse`) de operações do Claude. São ideais para guardrails arquiteturais e validação automática.

**Recomendação: crie os 4 hooks abaixo no setup inicial ou Wave 0.** Diferente das skills (que nascem da repetição), hooks de proteção arquitetural valem desde o dia 1 — previnem erros que são caros de corrigir depois.

#### Pré-requisito: instalar ktlint standalone

```bash
brew install ktlint
# ou: curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.5.0/ktlint && chmod +x ktlint
```

#### Hook 1: Domain Purity Guard (o mais importante)

Protege a regra #1 do projeto: `sm-domain` não pode importar Spring/JDBC/Kafka. Roda em < 50ms. **Bloqueia** a edição se violar.

**Arquivo**: `.claude/hooks/domain-purity-guard.sh`
```bash
#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Bloqueia imports de infraestrutura no módulo sm-domain
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só arquivos dentro de sm-domain/src/main
[[ "$FILE_PATH" != */sm-domain/src/main/*.kt ]] && exit 0

# Imports proibidos
FORBIDDEN=$(grep -nE '^import (org\.springframework|javax\.|jakarta\.|org\.apache\.kafka|java\.sql|org\.hibernate|io\.r2dbc)\.' "$FILE_PATH" 2>/dev/null)

if [[ -n "$FORBIDDEN" ]]; then
  echo "BLOQUEADO: sm-domain não pode ter dependências de infraestrutura!" >&2
  echo "$FORBIDDEN" >&2
  echo "sm-domain permite apenas: Kotlin stdlib + Jackson annotations" >&2
  exit 2
fi
exit 0
```

#### Hook 2: Domain Dependencies Guard

Protege no nível de dependências do Gradle — impede adicionar Spring/Kafka/JDBC ao `build.gradle.kts` do domain. Roda em < 50ms. **Bloqueia**.

**Arquivo**: `.claude/hooks/domain-deps-guard.sh`
```bash
#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Bloqueia dependências de infra no build.gradle.kts do sm-domain
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só o build.gradle.kts do sm-domain
[[ "$FILE_PATH" != */sm-domain/build.gradle.kts ]] && exit 0

FORBIDDEN=$(grep -nEi '(spring|kafka|jdbc|hibernate|jpa|r2dbc|servlet)' "$FILE_PATH" 2>/dev/null | grep -v "^.*//")

if [[ -n "$FORBIDDEN" ]]; then
  echo "BLOQUEADO: sm-domain/build.gradle.kts não pode ter dependências de infra!" >&2
  echo "$FORBIDDEN" >&2
  exit 2
fi
exit 0
```

#### Hook 3: Code Style (ktlint standalone)

Roda ktlint direto no arquivo editado (~1-3s). **Avisa** sem bloquear — o Claude corrige na sequência.

**Arquivo**: `.claude/hooks/post-edit-lint.sh`
```bash
#!/bin/bash
# PostToolUse matcher "Edit|Write"
# Roda ktlint standalone (NÃO via Gradle) no arquivo editado
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Só arquivos Kotlin
[[ "$FILE_PATH" != *.kt ]] && [[ "$FILE_PATH" != *.kts ]] && exit 0

# ktlint standalone no arquivo (~1-3s, aceitável)
RESULT=$(ktlint "$FILE_PATH" 2>&1)
if [[ $? -ne 0 ]]; then
  echo "⚠️  ktlint encontrou problemas em $(basename "$FILE_PATH"):" >&2
  echo "$RESULT" >&2
fi
exit 0
```

#### Hook 4: Pre-commit Guard

Bloqueia commit direto na main/master. Roda em < 100ms.

**Arquivo**: `.claude/hooks/pre-commit-guard.sh`
```bash
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
```

#### Configuração no settings.json

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "bash .claude/hooks/domain-purity-guard.sh" },
          { "type": "command", "command": "bash .claude/hooks/domain-deps-guard.sh" },
          { "type": "command", "command": "bash .claude/hooks/post-edit-lint.sh" }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "bash .claude/hooks/pre-commit-guard.sh" }
        ]
      }
    ]
  }
}
```

Não esqueça de torná-los executáveis:
```bash
chmod +x .claude/hooks/*.sh
```

#### Resumo dos hooks

| Hook | Tempo | Ação | Problema que resolve |
|------|-------|------|---------------------|
| **domain-purity-guard** | <50ms | Bloqueia | Import de infra no sm-domain |
| **domain-deps-guard** | <50ms | Bloqueia | Dependência de infra no build.gradle do domain |
| **post-edit-lint** | 1-3s | Avisa | Code style inconsistente |
| **pre-commit-guard** | <100ms | Bloqueia | Commit acidental na main |

**REGRA DE OURO**: hooks devem completar em < 5 segundos. Nunca rode Gradle, Docker ou processos pesados em hooks. Se precisa de mais, use uma skill.

### Passo 6.4 — Quando introduzir Agent Teams (waves paralelas)

**Use agentes paralelos quando**:
- A wave tem 4+ tasks independentes
- Você já tem o pipeline funcionando (pelo menos /test e /commit)
- O build está estável na main

**Como usar**:

```
Execute em paralelo com worktree isolation:
- T14 (Command Controllers): branch feat/t14-controllers
- T23 (Read Model): branch feat/t23-read-model
- T29 (Kafka Consumer): branch feat/t29-consumer

Cada agente deve:
1. Criar branch, implementar, testar
2. Commitar e criar PR
3. Reportar resultado
```

O Claude vai criar agentes com `isolation: "worktree"` e `run_in_background: true`.

**Após todos completarem**:
```bash
git checkout main && git pull && ./gradlew build
```

Se o build falhar, corrija antes da próxima wave.

### Passo 6.5 — Quando introduzir Agentes Especialistas

**Crie agentes especialistas quando**:
- Você perceber que repete as mesmas instruções de contexto para um tipo de task
- Exemplo: toda task do sm-domain precisa das mesmas regras, referências ao legado, e padrões

**Arquivo**: `.claude/agents/domain-specialist.md`
```yaml
---
name: domain-specialist
description: Especialista no módulo sm-domain.
---

# Domain Specialist
[contexto específico que você descobriu ao trabalhar neste módulo]
```

O conteúdo do agente é construído a partir da sua experiência REAL, não de templates vazios.

---

## PARTE 7: VALIDAÇÃO FINAL

### Passo 7.1 — Build completo
```bash
./gradlew clean build
```

### Passo 7.2 — Validação de compatibilidade

```
Compare o sistema novo com o legado:
1. Todos os 27 endpoints existem com os mesmos paths e métodos?
2. Todos os 24 eventos têm os mesmos campos JSON?
3. Os headers de multi-tenancy são processados?
4. As roles de segurança estão corretas?
5. O checkout faz as mesmas validações?
```

### Passo 7.3 — Testes de integração com serviços mockados

Configure WireMock para simular os serviços externos e rode o fluxo completo.

---

## Resumo: O que criar e QUANDO

| Quando | O que criar | Por quê |
|--------|-------------|---------|
| Dia 1 | Nada — só explorar o legado | Entender antes de agir |
| Dia 2 | docs/SPEC.md | Base de todas as decisões |
| Dia 3 | Issues no GitHub + CLAUDE.md + settings.json mínimo + .claude/rules/domain.md | Mínimo para começar |
| Wave 0 | Projeto Gradle, docker-compose | Foundation |
| Após 3º commit | Skill `/commit` | Eliminar repetição |
| Após 5º test run | Skill `/test` | Encapsular env vars |
| Setup inicial ou Wave 0 | 4 hooks: domain-purity-guard, domain-deps-guard, post-edit-lint, pre-commit-guard | Guardrails arquiteturais + code style + proteção de branch |
| Após pipeline manual 3x | Skill `/deliver` | Pipeline automático |
| Wave com 4+ tasks | Agent Teams + worktree | Paralelismo |
| Repetir contexto de módulo | Agentes especialistas | Reutilização de contexto |
| Repetir regras por módulo | .claude/rules/ por módulo | Contexto automático |

---

## Princípio Central

**Não otimize antes de sentir a dor.**

O Claude Code tem muitas features (skills, hooks, agents, rules, memory). Cada uma resolve um problema específico. Se você configurar tudo no dia 1, não sabe se está resolvendo problemas reais ou imaginários.

O fluxo correto é: **trabalhar → sentir atrito → criar a automação que elimina aquele atrito → repetir.**
