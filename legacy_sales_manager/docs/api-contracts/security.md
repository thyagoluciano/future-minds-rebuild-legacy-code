# Contratos de Segurança e Multi-Tenancy

---

## 1. Headers HTTP Obrigatórios

Todos os endpoints em `/purchase-orders/**` exigem os seguintes headers:

| Header | Obrigatório | Exemplo | Descrição |
|--------|-------------|---------|-----------|
| `X-Realwave-Organization-Slug` | **sim** | `my-tenant` | Identifica o tenant/organização. Determina qual schema do banco será usado. |
| `X-Realwave-Application-Id` | **sim** | `rw_sm_c` | Identifica a aplicação cliente. Validado contra o Keycloak. |
| `Authorization` | **sim** | `Bearer eyJhb...` | Token JWT emitido pelo Keycloak. |
| `X-Realwave-Tracking-Id` | não | `uuid-tracking` | Correlation ID para rastreamento global. |
| `X-Realwave-Tracking-Context` | não | `uuid-context` | Contexto de rastreamento dentro de um fluxo. |
| `X-Realwave-Channel` | não | `WEB` | Canal de origem da requisição. |
| `Content-Type` | sim (write) | `application/json;charset=UTF-8` | Obrigatório em POST/PUT. |

**Constantes no código** (de `RealwaveContextConstants`):
```
ORGANIZATION_SLUG_HEADER   → X-Realwave-Organization-Slug
APPLICATION_ID_HEADER      → X-Realwave-Application-Id
TRACKING_ID_HEADER         → X-Realwave-Tracking-Id
TRACKING_CONTEXT_HEADER    → X-Realwave-Tracking-Context
CHANNEL_CONTEXT_HEADER     → X-Realwave-Channel
```

---

## 2. Autenticação — Keycloak (Bearer Token)

O sistema usa **Keycloak** como provedor de identidade com fluxo Bearer-only.

### Configuração por módulo

| Módulo | `keycloak.resource` | Porta |
|--------|---------------------|-------|
| command-application | `rw_sm_c` | 8080 |
| query-application | `rw_sm_q` | 8180 |
| consumer | `rw_sm_consumer` | 8082 |

### Propriedades Keycloak
```properties
keycloak.authServerUrl=https://keycloak-dev.apirealwave.io/auth
keycloak.realm=zup
keycloak.bearerOnly=true
keycloak.sslRequired=external
security.enabled=true
security.basic.enabled=false
```

### Formato do Token
JWT Bearer padrão. O token deve:
- Ser emitido pelo realm `zup` no Keycloak configurado
- Estar no header `Authorization: Bearer <token>`
- Ser válido (não expirado) no momento da requisição

---

## 3. Multi-Tenancy

O sistema suporta **múltiplos tenants com isolamento por schema** no PostgreSQL.

### Como funciona

```
Request HTTP
  → X-Realwave-Organization-Slug: "tenant-abc"
  → X-Realwave-Application-Id: "rw_sm_c"
       ↓
  TenantValidationFilter (command) / MultiTenantFilter (query)
       ↓
  Valida presença dos headers
  Extrai organization slug
       ↓
  LiquibaseHandler.handleTenant("tenant-abc")
  → Cria schema "tenant_abc" no PostgreSQL se não existir
  → Executa migrations Liquibase no schema
       ↓
  RealwaveContextHolder.setContext(RealwaveContext)
  → Armazena em ThreadLocal para a requisição
       ↓
  DataSource roteia para schema do tenant
```

### Estrutura do RealwaveContext (ThreadLocal)
```kotlin
data class RealwaveContext(
    val organization: String,       // X-Realwave-Organization-Slug
    val application: String,        // X-Realwave-Application-Id
    val globalTrackingId: String?,  // X-Realwave-Tracking-Id
    val contextTrackingId: String?, // X-Realwave-Tracking-Context
    val channel: String?            // X-Realwave-Channel
)
```

### Prefixo do Tenant
```properties
tenant.prefix=rw_sm
```
O schema no banco é prefixado: `rw_sm_{organization-slug}`.

---

## 4. Filtros de Segurança

### TenantValidationFilter (command-application — porta 8080)

**Tipo:** `GenericFilterBean` (executado antes do Spring Security)

**Paths validados:**
```properties
paths.to.validate.headers=/purchase-orders
```

**Lógica:**
1. Se path começa com `/purchase-orders`
2. Verifica presença de `X-Realwave-Organization-Slug`
3. Verifica presença de `X-Realwave-Application-Id`
4. Se ausente: retorna `400 Bad Request`
5. Se presente: popula RealwaveContextHolder e passa para o próximo filtro

### MultiTenantFilter (query-application — porta 8180)

**Tipo:** `GenericFilterBean`

Mesma lógica do TenantValidationFilter, adicionalmente:
- Chama `LiquibaseHandler.handleTenant()` para garantir que o schema existe e está atualizado

---

## 5. Propagação de Contexto em Kafka

O `RealwaveContext` é propagado junto com as mensagens Kafka para manter o rastreamento entre serviços.

### No Producer (envelope Kafka)
```kotlin
// EventBuilderUtils.eventHeaderBuilder()
val context = RealwaveContextHolder.getContext()

// Header do envelope inclui:
{
  "eventId": "uuid",
  "eventType": "PurchaseOrderCreated",
  "timestamp": "2026-03-30T10:00:00",
  "domain": "SALES-MANAGER",
  "context": {
    "organization": "tenant-abc",
    "application": "rw_sm_c",
    "globalTrackingId": "tracking-uuid",
    "contextTrackingId": "context-uuid",
    "channel": "WEB"
  }
}
```

### No Consumer
```kotlin
// ParseEventUtils.loadContextVariables()
// Restaura RealwaveContext do header da mensagem Kafka
// Repopula RealwaveContextHolder para o processamento
```

---

## 6. Persistência do Contexto no Event Store

Ao salvar eventos no Event Store, o contexto é incluído como MetaData:

```kotlin
// PurchaseOrderEventRepositoryImpl.loadMetaData()
MetaData(
    organization = context.organization,
    application = context.application,
    globalTrackingId = context.globalTrackingId,
    contextTrackingId = context.contextTrackingId,
    channel = context.channel
)
```

Isso garante que cada evento no Event Store carregue qual tenant/canal o originou.

---

## 7. Respostas de Erro de Segurança

| Situação | Status | Descrição |
|----------|--------|-----------|
| Header `X-Realwave-Organization-Slug` ausente | `400` | Tenant não identificado |
| Header `X-Realwave-Application-Id` ausente | `400` | Aplicação não identificada |
| Token JWT ausente | `401` | Não autenticado |
| Token JWT inválido/expirado | `401` | Token rejeitado pelo Keycloak |
| Permissão insuficiente | `403` | Recurso não autorizado |

---

## 8. Reprodução no Rebuild (Spring Boot 3.x)

Para reproduzir este comportamento no rebuild com Spring Boot 3.x:

### Headers (manter idênticos)
Os headers `X-Realwave-*` devem ser mantidos **exatamente iguais** pois os sistemas que chamam o Sales Manager já os enviam.

### Autenticação
Substituir `spring-boot-starter-zup-iam` (Keycloak adapter descontinuado) por:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Configuração equivalente:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.apirealwave.io/auth/realms/zup
```

### Multi-Tenancy
Reimplementar `TenantValidationFilter` como `OncePerRequestFilter`:
```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TenantFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req, res, chain) {
        val org = req.getHeader("X-Realwave-Organization-Slug")
            ?: return res.sendError(400, "Missing X-Realwave-Organization-Slug")
        val app = req.getHeader("X-Realwave-Application-Id")
            ?: return res.sendError(400, "Missing X-Realwave-Application-Id")
        TenantContext.set(org, app)
        try { chain.doFilter(req, res) } finally { TenantContext.clear() }
    }
}
```
