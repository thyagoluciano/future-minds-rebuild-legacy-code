# Sales Manager Rebuild

Reconstrução do `realwave-sales-manager` (legado) com arquitetura limpa: CQRS + Event Sourcing customizado, multi-tenancy per-schema, Spring Boot 3.4.x. Compatibilidade 100% de contratos de API e comportamento de domínio com o sistema legado é obrigatória.

**Legado:** `/Users/zupper/Documents/develop/realwave/realwave-sales-manager`
**Análise legado:** `legacy_sales_manager/analise-completa.md`
**Spec técnica:** `docs/project-spec.md` — consultar antes de implementar qualquer classe.

---

## Tech Stack

| Camada        | Tecnologia                                 |
|---------------|--------------------------------------------|
| Linguagem     | Kotlin 2.1.x + Java 21                     |
| Framework     | Spring Boot 3.4.x                          |
| Build         | Maven (multi-module POM) — **não migrar para Gradle** |
| Banco         | PostgreSQL 16 + JDBC (sem JPA/Hibernate)   |
| Mensageria    | Kafka (Spring Kafka)                       |
| Auth          | spring-boot-starter-oauth2-resource-server |
| HTTP clients  | Spring Cloud OpenFeign                     |
| Migrations    | Liquibase (per-tenant)                     |
| Testes        | MockK + Testcontainers                     |

---

## Estrutura de Módulos

```
sales-manager/                  # Parent POM
├── domain/                     # Kotlin puro — ZERO Spring
├── api/                        # Contratos REST (Spring MVC annotations, DTOs)
├── infrastructure/             # Spring + PostgreSQL + Kafka + Feign + Liquibase
├── query/                      # Projeção CQRS (read side)
├── command-app/                # Spring Boot — write (porta 8080)
├── query-app/                  # Spring Boot — read (porta 8180)
└── consumer-app/               # Spring Boot — Kafka consumer (porta 8082)
```

### Dependências entre módulos

```
domain          → kotlin-stdlib, jackson-annotations (ZERO Spring)
api             → domain, spring-web
infrastructure  → domain, api, spring-boot-starter-*, spring-kafka, liquibase, postgresql
query           → domain, api, infrastructure
command-app     → domain, api, infrastructure
query-app       → query, api, infrastructure
consumer-app    → infrastructure
```

**Regra absoluta do `domain/`:** nenhuma dependência de framework. Contratos REST, DTOs e validators ficam em `api/`. Controllers implementam as interfaces de `api/`.

---

## Comandos de Build

```bash
# Build completo
mvn clean install

# Build sem testes
mvn clean install -DskipTests

# Rodar testes de um módulo
mvn test -pl domain
mvn test -pl command-app

# Rodar aplicação (command side)
mvn spring-boot:run -pl command-app

# Rodar aplicação (query side)
mvn spring-boot:run -pl query-app
```

---

## Convenções de Código

- **Kotlin idiomático:** data classes, sealed classes, extension functions — sem boilerplate Java
- **Imutabilidade:** preferir `val` sobre `var`; coleções imutáveis na interface pública
- **Aggregate:** eventos via `applyChange(event)` — nunca mutação direta de estado
- **Value Objects:** `data class` com validação no `init { require(...) }`
- **Nomes:** `PascalCase` para classes, `camelCase` para funções/propriedades, `UPPER_SNAKE` para enum values
- **Pacote base:** `br.com.zup.realwave.sales.manager.<módulo>`
- **Sem anotações Spring no `domain/`:** nenhum `@Component`, `@Service`, `@Autowired`
- **Testes unitários:** MockK (não Mockito); testes de integração com Testcontainers

---

## Formato de Commit

```
<tipo>(<escopo>): <descrição curta>

Tipos: feat | fix | refactor | test | docs | chore
Escopo: domain | api | infra | query | command-app | query-app | consumer-app

Exemplos:
feat(domain): add CheckoutCommand and PurchaseOrderCheckedOut event
fix(infra): correct tenant schema resolution in JdbcEventStore
test(command-app): add integration test for checkout endpoint
```

---

## Links

- **Issues:** https://github.com/thyagoluciano/sales-manager-rebuild/issues
- **Projeto legado (referência):** `/Users/zupper/Documents/develop/realwave/realwave-sales-manager`
- **Spec técnica:** `docs/project-spec.md`
- **Plano de migração:** `docs/migration-plan.md`

---

## Multi-Tenancy

Cada tenant tem schema isolado `rw_sm_{tenant}`. A tabela `domain_events` fica dentro do schema do tenant — mesmo padrão das tabelas de query. Nunca usar schema compartilhado.

## Compatibilidade com Legado

Os contratos de API (endpoints, payloads JSON, códigos de status HTTP) devem ser idênticos ao legado. Em caso de dúvida, consultar `legacy_sales_manager/analise-completa.md` ou o código-fonte em `/Users/zupper/Documents/develop/realwave/realwave-sales-manager`.
