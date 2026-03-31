# Plano de Migração: Realwave Sales Manager Rebuild

## Contexto

O **Realwave Sales Manager** é um microsserviço backend-to-backend que gerencia o ciclo de vida completo de **Purchase Orders** (pedidos de compra) em um contexto de telecom/serviços digitais. O sistema legado está em tecnologias severamente desatualizadas (Spring Boot 1.5.3, Java 8, Kotlin 1.2.50) e depende de **16+ bibliotecas proprietárias** que não existem em repositórios públicos. O objetivo é reconstruir o sistema mantendo o comportamento de negócio e compatibilidade total de API (request e response).

---

## 1. Stack Alvo

| Componente | Tecnologia | Substitui |
|---|---|---|
| Linguagem | **Kotlin 2.1.x** | Kotlin 1.2.50 |
| JVM | **Java 21 LTS** | Java 8 |
| Framework | **Spring Boot 3.4.x** | Spring Boot 1.5.3 |
| Build | **Maven** | Maven (mantido) |
| Event Store | **PostgreSQL (tabela domain_events, per-tenant schema)** | Greg Young's EventStore + event-sourcing-core |
| Query DB | **PostgreSQL 16+** | PostgreSQL (mesmo) |
| Messaging | **Spring Kafka 3.x** | Spring Kafka 1.3.2 |
| HTTP Clients | **Spring Cloud OpenFeign 4.x** | OpenFeign 9.5.0 + wrappers proprietários |
| Auth | **spring-boot-starter-oauth2-resource-server** | zup-iam + Keycloak adapter (descontinuado) |
| Migrations | **Liquibase 4.29.x** | Liquibase 3.5.3 |
| Observabilidade | **Micrometer + OpenTelemetry** | realwave-graylog + Prometheus 0.0.25 |
| Serialização | **Jackson + jackson-module-kotlin** | realwave-kserialize |
| Testes | **JUnit 5 + MockK + Testcontainers** | JUnit 4 |

### Decisão: Kotlin (não Java)
Todo o codebase legado é Kotlin com uso intensivo de data classes, extension functions e null-safety. Reescrever em Java perderia expressividade sem ganho real.

### Decisão: Event Sourcing customizado (não Axon Framework)
- Existe apenas **1 aggregate** (PurchaseOrder) com 25 eventos — Axon é overkill
- Multi-tenancy per-tenant schema requer controle total sobre o schema do event store
- Implementação customizada requer ~150 linhas de Kotlin (5 classes)

---

## 2. Estrutura de Módulos (12 → 7)

```
sales-manager/
├── pom.xml                        # Parent POM (dependency management, plugins)
│
├── domain/                        # Kotlin puro, ZERO dependências Spring
│   ├── PurchaseOrder (aggregate root)
│   ├── AggregateRoot<ID> base class
│   ├── 25 domain events (implementam DomainEvent.apply())
│   ├── Value objects (Item, Payment, Freight, Customer, etc.)
│   ├── Commands
│   ├── Ports (interfaces de serviços de domínio e repositórios)
│   └── Regras de validação
│
├── api/                           # Contratos REST (depende de Spring MVC)
│   ├── Interfaces REST (@RequestMapping, @GetMapping, etc.)
│   ├── Request DTOs (ItemRequest, PaymentRequest, etc.)
│   ├── Response DTOs (PurchaseOrderResponse, etc.)
│   └── Validators de request (ItemRequestValidator, etc.)
│
├── infrastructure/                # Spring + concerns externos
│   ├── JdbcEventStore (PostgreSQL domain_events, per-tenant schema)
│   ├── Transactional Outbox + Kafka Producer
│   ├── 6 Feign clients (CMS, PCM, CIM, Coupon, MGM, COM, Callback)
│   ├── Multi-tenancy (TenantFilter, AbstractRoutingDataSource, Liquibase)
│   ├── Security (OAuth2 Resource Server)
│   ├── Request Context (TenantContext, Feign interceptors)
│   ├── Error handling (@RestControllerAdvice)
│   └── Jackson config
│
├── query/                         # Projeção query-side + read API
│   ├── 25 event handlers (event → query DB)
│   ├── JDBC repositories (read models)
│   ├── Query REST controllers (4 endpoints, implementam interfaces do api/)
│   └── Liquibase migrations (query schema per-tenant)
│
├── command-app/                   # Spring Boot app - lado write (port 8080)
│   ├── 25 Command REST controllers (implementam interfaces do api/)
│   ├── 14 Command handlers
│   └── Depende de: domain, api, infrastructure
│
├── query-app/                     # Spring Boot app - lado read (port 8180)
│   ├── Application config
│   └── Depende de: query, api, infrastructure
│
└── consumer-app/                  # Spring Boot app - Kafka consumer (port 8082)
    ├── @KafkaListener
    ├── CallbackService
    └── Depende de: infrastructure
```

### Separação Domain vs API

O módulo `domain/` é **Kotlin puro** — sem nenhuma dependência Spring. Contém apenas:
- Aggregate, events, value objects, commands, ports (interfaces)
- Dependências permitidas: kotlin-stdlib, jackson-annotations (para serialização de eventos)

O módulo `api/` contém os **contratos REST** com anotações Spring MVC:
- Interfaces anotadas com `@RequestMapping`, `@PostMapping`, etc.
- DTOs de Request e Response (compatíveis 1:1 com o legado)
- Validators de request
- Depende de: Spring Web, domain (para tipos de domínio usados nos DTOs)

Os módulos `command-app/` e `query-app/` **implementam** as interfaces definidas em `api/`.

### Mapeamento legado → novo

| Módulos legados | Novo módulo | Razão |
|---|---|---|
| `domain` | `domain/` | Mantém puro, sem Spring |
| `api` | `api/` | Contratos REST separados do domínio (contém anotações Spring MVC) |
| command-repository + producer + integration + infrastructure + events | `infrastructure/` | Todas são concerns de infraestrutura |
| query-repository + query-event-handler | `query/` | Event handlers e repositórios de escrita são fortemente acoplados |
| command-application | `command-app/` | Mantém como deployable |
| query-application | `query-app/` | Mantém como deployable |
| consumer | `consumer-app/` | Mantém como deployable |

---

## 3. Substituição de Dependências Proprietárias

### Alta Prioridade (risco alto)

| Dependência | Substituição |
|---|---|
| `event-sourcing-core` | `AggregateRoot<ID>` customizado (~150 linhas Kotlin) com `applyChange()`, `pendingEvents`, `replayEvent()` |
| `event-store-connector` | `JdbcEventStore` escrevendo em tabela `domain_events` (aggregate_id, event_type, payload JSONB, metadata JSONB, version, created_at) |
| `relational-database-connector` | Spring JDBC + Liquibase |
| `spring-tenant` | `TenantFilter` (OncePerRequestFilter) + `TenantContext` (ThreadLocal) + `AbstractRoutingDataSource` |

### Média Prioridade

| Dependência | Substituição |
|---|---|
| `spring-boot-starter-zup-iam` | `spring-boot-starter-oauth2-resource-server` com `issuer-uri: keycloak.../realms/zup` |
| `realwave-context-web` | `TenantContext` ThreadLocal + `RequestInterceptor` Feign |
| `realwave-exception-handler` | `@RestControllerAdvice` mantendo formato `{errors: [{code, message}]}` |

### Baixa Prioridade

| Dependência | Substituição |
|---|---|
| `realwave-graylog` | Logback + `logstash-logback-encoder` |
| `realwave-kserialize` / `realwave-serialize` | `jackson-module-kotlin` (auto-configured) |
| `realwave-feign-commons` | Spring Cloud OpenFeign + `RequestInterceptor` customizado |
| Client SDKs (cms, pcm, cim, coupon) | Novas `@FeignClient` interfaces com DTOs locais |

---

## 4. Estratégia de Event Sourcing

### Event Store: PostgreSQL (per-tenant schema)

```sql
-- Criada dentro de cada schema rw_sm_{tenant}
CREATE TABLE domain_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(36) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    metadata        JSONB,  -- channel, tracking IDs
    version         BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (aggregate_id, version)
);
CREATE INDEX idx_domain_events_aggregate ON domain_events(aggregate_id, version);
```

**Multi-tenancy no event store:** Per-tenant schemas, consistente com toda a aplicação. A tabela `domain_events` é criada dentro de cada schema `rw_sm_{tenant}`, seguindo o mesmo padrão que o query-side. O `LiquibaseHandler` gerencia a criação de schemas e execução de migrations para cada tenant, tanto para o event store quanto para as tabelas de query.

### Sincronização Event Store → Query DB: Transactional Outbox

1. Command handler salva eventos em `domain_events`
2. Na mesma transação, insere na tabela `outbox` (também per-tenant)
3. Poller publica para Kafka e marca como publicado
4. Query-app consome de Kafka e projeta no query DB

Isso garante **at-least-once delivery** sem transações distribuídas.

### Event Serialization

Eventos serializados como JSONB na coluna `payload`. A coluna `event_type` armazena o discriminador (ex: `PurchaseOrderCreated`). Um registry mapeia tipos para deserializers:

```kotlin
object EventTypeRegistry {
    private val types: Map<String, KClass<out DomainEvent>> = mapOf(
        "PurchaseOrderCreated" to PurchaseOrderCreated::class,
        "PurchaseOrderCheckedOut" to PurchaseOrderCheckedOut::class,
        // ... 25 entries
    )
    fun resolve(type: String): KClass<out DomainEvent> = types[type] ?: error("Unknown event: $type")
}
```

### Snapshotting (otimização futura)

Não necessário para release inicial. A maioria dos purchase orders terá menos de 30 eventos. Se performance se tornar problema, adicionar tabela `snapshots` que armazena o aggregate serializado em uma dada versão.

---

## 5. Fases de Migração

### Fase 0: Fundação (2 semanas)
- Scaffold do projeto Maven multi-module (Kotlin 2.x, Spring Boot 3.4.x, Java 21) com 7 módulos
- `domain/` com todos value objects, enums, 25 eventos, aggregate PurchaseOrder (Kotlin puro, sem Spring)
- `api/` com interfaces REST e DTOs de request/response (compatíveis 1:1 com legado)
- Classe base `AggregateRoot` e interface `DomainEvent`
- Testes unitários para todas as transições de estado e validações
- **Checkpoint:** Modelo de domínio compila e todos os testes passam

### Fase 1: Infraestrutura Core (2 semanas)
- `JdbcEventStore` com tabela `domain_events` (per-tenant schema)
- `TenantContext` + `TenantFilter` + `AbstractRoutingDataSource`
- Liquibase migrations (event store + query schemas, per-tenant)
- Security (OAuth2 Resource Server)
- Jackson config + Error handling
- **Checkpoint:** Salvar e carregar PurchaseOrder via event store, com multi-tenancy per-tenant

### Fase 2: Command Application (2 semanas)
- 25 REST endpoints compatíveis com API legada (request/response idênticos)
- 14 command handlers
- 6 Feign clients (CMS, PCM, CIM, Coupon, MGM, COM)
- Kafka producer com formato de envelope idêntico ao legado
- Transactional outbox (per-tenant)
- **Correções:** remover dupla validação no checkout; coupon checkout iterar todos os items
- **Checkpoint:** Command app cria, modifica, valida e faz checkout de pedidos

### Fase 3: Query Application (1.5 semanas)
- 25 event handlers projetando para PostgreSQL (per-tenant schema)
- JDBC repositories para todas as tabelas de query
- Liquibase migrations do query schema (portadas do legado)
- 4 GET endpoints compatíveis com API legada (response idêntico)
- **Checkpoint:** Ciclo CQRS completo funciona end-to-end

### Fase 4: Consumer Application (1 semana)
- `@KafkaListener` consumindo de `rw_sm_purchase_events`
- `CallbackService` despachando POST para URLs dinâmicas de callback
- **Checkpoint:** Sistema completo funciona: create → checkout → COM callback → callback externo

### Fase 5: Testes End-to-End (1.5 semanas)
- Testes E2E cobrindo todo o ciclo de vida (create, assemble, validate, checkout, COM callback, query)
- Testes de performance (replay de eventos < 100ms para 50 eventos)
- Testes de compatibilidade de contrato (JSON request/response idênticos ao legado)
- Testes de isolamento multi-tenant (per-tenant schema)
- Decisão sobre status `FAILED` (usar ou documentar que rollback-to-OPENED é intencional)

### Fase 6: Migração de Dados (1-2 semanas, paralelo com Fase 5)
- Script de exportação do EventStore legado via HTTP API
- Transformação de eventos para formato `domain_events`
- Carga no PostgreSQL nos per-tenant schemas corretos
- Rebuild do query DB via replay de eventos
- Verificação de consistência (estado reconstruído vs snapshot legado)

### Fase 7: Canary Deployment (1-2 semanas)
- Deploy ao lado do legado com traffic router
- Rampa: 1% → 5% → 25% → 50% → 100%
- Monitoramento de error rates, latência e consistência
- Rollback plan: reverter tráfego ao legado se houver problemas

**Timeline total estimada: 11-14 semanas**

---

## 6. Correções de Bugs Conhecidos

| Bug | Correção |
|---|---|
| Dupla validação no checkout | Chamar `validate()` apenas uma vez em `CheckoutCommand.execute()` |
| Coupon checkout hardcoded para primeiro item | Iterar todos os items, não apenas `items[0].offerItems[0]` |
| Status FAILED nunca usado | Decidir: usar para COM FAILED ou documentar que rollback-to-OPENED é intencional |
| Naming inconsistency Kafka (`Checkedout` vs `CheckedOut`) | Manter compatibilidade com `Checkedout` no Kafka para não quebrar consumers |

---

## 7. Estratégia de Testes

| Nível | Escopo | Ferramentas |
|---|---|---|
| Unit | 25 eventos apply(), validações, state machine, cálculo de descontos | JUnit 5 + MockK |
| Integration | EventStore, TenantFilter, Liquibase, OAuth2, Feign clients | Testcontainers + WireMock |
| Contract | Request/response JSON de todos endpoints vs docs legados | Spring MockMvc + JSON assertions |
| E2E | Ciclo completo com PostgreSQL + Kafka em containers | Testcontainers |
| Performance | Replay de eventos, throughput | JMeter ou k6 |

---

## 8. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Incompatibilidade de serialização de eventos | Eventos migrados não deserializam | Suite de testes de deserialização com amostras reais do EventStore legado (Fase 0) |
| Regressão multi-tenancy | Vazamento de dados entre tenants | Testes de isolamento com múltiplos tenants concorrentes em per-tenant schemas |
| Diferenças no fluxo de checkout | Pedidos falham ou produzem resultados diferentes | Portar todos os testes de integração legados; shadow traffic em produção |
| Complexidade de export do EventStore | Não conseguir exportar todos os eventos | Prototipar script de export na Fase 0 com dados de um tenant |
| Mudanças comportamentais do Spring Security 6 | Regressões de auth (401/403 inesperados) | Testar todos endpoints com tokens válidos/inválidos/expirados |

---

## 9. Migração de Dados

### Desafio
O sistema legado armazena eventos no Greg Young's EventStore. O novo sistema usa PostgreSQL `domain_events` em per-tenant schemas. Todos os eventos históricos devem ser migrados para manter auditabilidade e capacidade de replay.

### Estratégia: Event Replay Export

1. **Exportar do EventStore:** Script de migração que lê todos os event streams via HTTP API (`GET /streams/{stream-id}`). Cada stream corresponde a um PurchaseOrder aggregate.
2. **Transformar:** Mapear cada evento legado para o novo formato. O payload permanece largamente inalterado (domain model preservado). Transformação principal: strip metadata do EventStore e adicionar colunas da tabela `domain_events`.
3. **Carregar no PostgreSQL:** Inserir na tabela `domain_events` no per-tenant schema correto (`rw_sm_{tenant}`). Manter ordenação e versões originais dos eventos.
4. **Verificar:** Para cada aggregate migrado, replay eventos no novo sistema e comparar estado resultante com o snapshot do query DB legado.
5. **Rebuild Query DB:** O query DB pode ser reconstruído inteiramente via replay de todos os eventos pelos event handlers. Isso também valida que os novos handlers produzem estado idêntico.

### Abordagem de Cutover

**Blue-green com event replay:**
1. Freeze do sistema legado (read-only ou maintenance window)
2. Export de todos os eventos do EventStore
3. Carga no PostgreSQL `domain_events` (per-tenant)
4. Replay de todos os eventos para rebuild do query DB
5. Verificação de consistência
6. Switch DNS/load balancer para novo sistema
7. Monitor por 24-48 horas
8. Decommission do legado

---

## Arquivos Críticos de Referência

- `legacy_sales_manager/analise-completa.md` — Análise completa com todas as regras de domínio, transições de estado, validações e códigos de erro
- `legacy_sales_manager/docs/api-contracts/command-endpoints.md` — 25 endpoints de comando com JSON exato de request/response
- `legacy_sales_manager/docs/api-contracts/query-endpoints.md` — 4 endpoints de query com estrutura completa do PurchaseOrderResponse
- `legacy_sales_manager/docs/api-contracts/security.md` — Headers, Keycloak config, multi-tenancy e guia de rebuild para Spring Boot 3.x
- `legacy_sales_manager/docs/events/domain-events.md` — 25 tipos de eventos com data class Kotlin e tabela Command→Event→Efeito
- `legacy_sales_manager/docs/events/kafka-config.md` — Formato do envelope Kafka, config do producer/consumer

## Verificação

Para validar que o rebuild está correto:
1. **Contract tests:** Comparar JSON de request/response de cada endpoint com a documentação legada (compatibilidade total)
2. **Event replay:** Exportar eventos de um tenant do EventStore legado, carregar no novo PostgreSQL (per-tenant schema), verificar que o estado reconstruído é idêntico
3. **Shadow traffic:** Rodar novo sistema em paralelo com o legado, comparar respostas
4. **Multi-tenant isolation:** Criar 2+ tenants com per-tenant schemas, operar simultaneamente, verificar que não há vazamento de dados
