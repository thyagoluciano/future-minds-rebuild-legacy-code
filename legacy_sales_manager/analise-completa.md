# Análise Completa: Realwave Sales Manager (Legacy)

> Projeto original: `/Users/zupper/Documents/develop/realwave/realwave-sales-manager`
> Data da análise: 2026-03-30

---

## 1. O QUE O SISTEMA FAZ — Problema de Negócio

O **Realwave Sales Manager** é um microserviço de gestão de vendas implementado com **CQRS + Event Sourcing** em Kotlin/Spring Boot 1.5. Centraliza e valida o fluxo de vendas, gerenciando **Purchase Orders** do estágio inicial até o checkout com integração ao **Customer Order Manager**.

### O que faz:
- Gerencia o ciclo completo de um pedido: criação → montagem (itens, pagamento, frete, promoções) → validação → checkout
- Mantém auditoria completa via Event Sourcing (append-only)
- Segrega Command (escrita) e Query (leitura) via CQRS com sincronização por Kafka
- 12 módulos, 25 tipos de eventos, 25+ commands, 5 integrações externas via Feign
- Multi-tenant com validação de contexto por requisição
- Callbacks assíncronos para notificação de status

### O que NÃO faz:
- Não processa pagamentos
- Não queima cupons ou convites MGM (Member-Get-Member)
- Não provisiona ou atualiza recursos envolvidos no processo de vendas

---

## 2. USUÁRIOS / CLIENTES

Sistema **backend-to-backend** (sem usuário humano direto). Clientes:

- **Customer Order Manager** — recebe o pedido no checkout
- **Catalog Manager (CMS/PCM)** — valida ofertas e produtos
- **CIM API** — validação de clientes
- **Coupon Service** — valida e aplica cupons
- **Member-Get-Member Service** — promoções referral
- **Canais de venda** (Web, Mobile, Sales Force) — originam os pedidos via REST API

---

## 3. CONCEITOS DE DOMÍNIO CENTRAIS

### 3.1 Aggregate Root: `PurchaseOrder`

Pacote: `br.com.zup.realwave.sales.manager.domain.PurchaseOrder`

Implementa `AggregateRoot` (Event Sourcing). Contém todo o estado de um pedido:

```kotlin
var status: PurchaseOrderStatus          // OPENED, CHECKED_OUT, COMPLETED, FAILED, CANCELED, DELETED
var type: PurchaseOrderType?
var customer: Customer?
var items: MutableSet<Item>
var payment: Payment
var freight: Freight?
var coupon: CouponCode?
var customerOrder: CustomerOrder?        // retornado pelo CustomerOrderManager no checkout
var callback: Callback?
var segmentation: Segmentation?
var salesForce: SalesForce?
var mgm: Mgm?
var onBoardingSale: OnBoardingSale?
var protocol: String?
var subscriptionId: String?
var channelCreate: Channel?
var channelCheckout: Channel?
var reason: Reason?
var installationAttributes: HashMap<ProductTypeId, InstallationAttribute>
```

### 3.2 Entidade: `Item`

Representa um produto/oferta no pedido:
- `catalogOfferId`, `catalogOfferType`
- `price`, `validity`, `offerFields`, `customFields`
- `pricesPerPeriod` — suporta preços variáveis por período
- `quantity`

### 3.3 Value Objects

| Value Object | Descrição |
|---|---|
| `PurchaseOrderId` | Identificador único do pedido |
| `CatalogOfferId` | ID da oferta no catálogo |
| `CatalogOfferType` | Tipo da oferta |
| `ProductId` / `ProductTypeId` | Identificadores de produto |
| `Price` | Moeda, valor, escala decimal |
| `PricePerPeriod` | Preço variável por período |
| `Channel` | Canal de criação/checkout (WEB, MOBILE, etc.) |
| `Callback` | URL e headers para notificação assíncrona |
| `Reason` | Código e descrição de razão |
| `SecurityCode` | Código de segurança informado no checkout |
| `CouponCode` | Código do cupom aplicado |
| `SalesForce` | ID e nome do vendedor |
| `Segmentation` | Query JSON para segmentação |
| `Mgm` | Código MGM e custom fields |
| `OnBoardingSale` | Oferta de onboarding vinculada |
| `InstallationAttribute` | Atributos de instalação por tipo de produto |
| `Customer` | `data class Customer(val id: String)` |

### 3.4 Status do Pedido (`PurchaseOrderStatus`)

```
OPENED → CHECKED_OUT → COMPLETED
                     ↘ FAILED
       ↘ CANCELED
       ↘ DELETED
```

### 3.5 Eventos de Domínio (25 eventos)

Todos herdam de `PurchaseOrderApplicableEvent` e implementam `apply(purchaseOrder)`:

| Evento | Trigger |
|---|---|
| `PurchaseOrderCreated` | POST /purchase-orders |
| `PurchaseOrderCheckedOut` | POST /{id}/checkout |
| `PurchaseOrderDeleted` | DELETE /{id} |
| `PurchaseOrderStatusUpdated` | Mudança de status |
| `PurchaseOrderItemAdded` | POST /{id}/items |
| `PurchaseOrderItemRemoved` | DELETE /{id}/items/{itemId} |
| `PurchaseOrderItemUpdated` | PUT /{id}/items/{itemId} |
| `PurchaseOrderPaymentUpdated` | POST /{id}/payment |
| `PurchaseOrderFreightUpdated` | POST /{id}/freight |
| `PurchaseOrderCouponUpdated` | POST /{id}/coupon |
| `PurchaseOrderCustomerUpdated` | POST /{id}/customer |
| `PurchaseOrderCustomerOrderUpdated` | Atualização do CustomerOrder |
| `PurchaseOrderMgmUpdated` | POST /{id}/mgm |
| `PurchaseOrderMgmDeleted` | DELETE /{id}/mgm |
| `PurchaseOrderSegmentationUpdated` | POST /{id}/segmentation |
| `PurchaseOrderOnBoardingSaleUpdated` | POST /{id}/onboarding-sale |
| `PurchaseOrderSalesForceUpdated` | POST /{id}/sales-force |
| `PurchaseOrderSalesForceRemoved` | DELETE /{id}/sales-force |
| `PurchaseOrderSubscriptionUpdated` | POST /{id}/subscription |
| `PurchaseOrderProtocolUpdated` | POST /{id}/protocol |
| `PurchaseOrderInstallationAttributesUpdated` | POST /{id}/installation-attributes |
| `PurchaseOrderInstallationAttributesDeleted` | DELETE /{id}/installation-attributes |
| `PurchaseOrderTypeUpdated` | POST /{id}/type |
| `PurchaseOrderReasonStatusUpdated` | Atualização razão/status |

---

## 4. ARQUITETURA TÉCNICA

### 4.1 Padrão: CQRS + Event Sourcing

```
┌──────────────────────────────────────────┐
│          REST Controllers                │
│   PurchaseOrderCommandApi (write)        │
│   PurchaseOrderQueryApi (read)           │
└──────────┬─────────────────┬────────────┘
           │                 │
    ┌──────▼──────┐   ┌──────▼──────┐
    │ COMMAND     │   │ QUERY       │
    │ SIDE        │   │ SIDE        │
    │ (Write)     │   │ (Read)      │
    │             │   │             │
    │ Commands    │   │ Repositories│
    │ Handlers    │   │ JDBC        │
    │ Domain      │   │ PostgreSQL  │
    │ Events      │   │             │
    │ Event Store │   │             │
    └──────┬──────┘   └──────▲──────┘
           │                 │
           └────── Kafka ────┘
              rw.sales.manager.events
```

### 4.2 Stack Tecnológica

| Tecnologia | Versão | Uso |
|---|---|---|
| Kotlin | 1.2.50 | Linguagem principal |
| Java | 8 | Suporte |
| Spring Boot | 1.5.3 | Framework |
| Spring Cloud Edgware | — | Microservices |
| Spring Kafka | 1.3.2 | Message broker |
| Feign | 9.5.0 | HTTP clients declarativos |
| PostgreSQL | 42.2.2 | Query database |
| Liquibase | 3.5.3 | Migrations |
| HikariCP | 2.6.1 | Connection pooling |
| Jackson | 2.9.3 | JSON |
| Hibernate Validator | 5.3.5 | Validação |
| Prometheus | 0.0.25 | Métricas |
| Log4j | 2.6.2 | Logging |
| Event Sourcing Core | (proprietary) | Framework de Event Sourcing |
| Mockk | 1.6 | Testes |

---

## 5. CASOS DE USO COMPLETOS

### Commands (Write) — 25 commands

#### Pedido Principal
- `CreatePurchaseOrderCommand` — cria novo pedido
- `UpdatePurchaseOrderType` — altera tipo do pedido
- `DeletePurchaseOrderCommand` — deleta pedido (status = DELETED)
- `ValidatePurchaseOrder` — valida pedido antes do checkout
- `CheckoutCommand` — realiza checkout (OPENED → CHECKED_OUT)

#### Itens
- `AddItemCommand` — adiciona item ao pedido
- `RemoveItemCommand` — remove item
- `UpdateItemCommand` — atualiza item existente

#### Pagamento / Frete / Cupom / Cliente
- `UpdatePaymentCommand` — configura métodos de pagamento
- `UpdateFreightCommand` — configura frete
- `UpdateCouponCommand` / `CreatePurchaseOrderCouponCommand` — gerencia cupom
- `UpdateCustomerCommand` — atualiza cliente
- `UpdateCustomerOrderCommand` — atualiza CustomerOrder após integração

#### Promoções / Vendas
- `UpdateMgmCommand` / `DeleteMgmCommand` — gerencia MGM
- `UpdateSalesForceCommand` / `RemoveSalesForceCommand` — gerencia vendedor
- `UpdateSegmentationCommand` — aplica segmentação/targeting
- `UpdateOnBoardingSaleCommand` — vincula oferta de onboarding

#### Instalação / Técnico
- `UpdateInstallationAttributesCommand` / `DeleteInstallationAttributesCommand`
- `UpdateProtocolCommand` — define protocolo/referência
- `UpdateSubscriptionCommand` — vincula subscription ID

### Queries (Read)

| Query | Endpoint |
|---|---|
| Buscar por ID | `GET /purchase-orders/{id}` |
| Buscar por protocolo | `GET /purchase-orders/{protocol}/protocol` |
| Buscar status | `GET /purchase-orders/{id}/status` |
| Buscar por cliente | `GET /purchase-orders?customerId=xxx&status=CHECKED_OUT` |

---

## 6. QUERY EVENT HANDLERS (Sincronização Kafka → Query DB)

25 handlers herdam de `BaseEventHandler<T>`. Cada um recebe:
- `event: T` — evento de domínio
- `metaData: MetaData` — timestamp, versão
- `version: AggregateVersion` — versão do evento

Repositórios disponíveis em todos os handlers:
- `purchaseOrderRepository`
- `customerOrderRepository`
- `paymentRepository`
- `freightRepository`
- `discountRepository`
- `installationAttributesRepository`
- `purchaseOrderItemRepository`

---

## 7. ESTRUTURA DE DTOs

### Requests
`PurchaseOrderRequest`, `ItemRequest`, `PaymentRequest`, `FreightRequest`,
`CouponRequest`, `CustomerRequest`, `CheckoutRequest`, `MgmRequest`,
`OnBoardingSaleRequest`, `ProtocolRequest`, `SalesForceRequest`,
`SegmentationRequest`, `SubscriptionRequest`, `InstallationAttributesRequest`

### Responses
`PurchaseOrderResponse`, `CreatePurchaseOrderResponse`, `CheckoutResponse`,
`PurchaseOrderStatusResponse`, `DeleteResponse`, `PurchaseOrderItemResponse`,
`UpdatePaymentResponse`, `UpdateFreightResponse`, `UpdateCustomerIdResponse`,
`UpdateCouponResponse`, `ProtocolResponse`, `PurchaseOrderMgmResponse`,
`PurchaseOrderSalesForceResponse`, `PurchaseOrderTypeResponse`,
`SegmentationResponse`, `SubscriptionResponse`, `UpdateOnBoardingSaleResponse`,
`UpdateInstallationAttributesResponse`, `DeleteInstallationAttributesResponse`

---

## 8. VALIDAÇÃO NO CHECKOUT

`PurchaseOrderValidator.validate(purchaseOrder)` verifica:
1. Presença de itens
2. Cliente ativo (via CustomerInfoService)
3. Métodos de pagamento válidos
4. Tipos de produto contra catálogo (via CatalogManagerService)
5. Cupom válido (via CouponService) — se aplicado
6. MGM válido (via MemberGetMemberService) — se presente

---

## 9. SEGURANÇA E MULTI-TENANCY

- `RealwaveContextHolder.getContext()` — extrai contexto do tenant
- `TenantValidationFilter` — valida tenant antes de processar
- Spring IAM Integration — autenticação/autorização
- Todos os commands passam pelo contexto do tenant

---

## 10. TRATAMENTO DE ERROS

| Exception Handler | Escopo |
|---|---|
| `PurchaseOrderExceptionHandler` | Erros gerais |
| `CheckoutExceptionHandler` | Erros durante checkout |
| `CouponExceptionHandler` | Erros de cupom |
| `CatalogManagerSearchExceptionHandler` | Erros de integração com catálogo |

Exceções customizadas:
- `PurchaseOrderValidationException`
- `CustomerInactiveException`
- Decoders específicos para erros Feign

---

## 11. MÓDULOS — RESPONSABILIDADES DETALHADAS

### 11.1 Tabela de Módulos

| Módulo | Porta | Tipo | Responsabilidade |
|---|---|---|---|
| `realwave-sales-manager-api` | — | Lib | Contratos REST: interfaces de controllers e todos os DTOs de request/response |
| `realwave-sales-manager-domain` | — | Lib | Núcleo de negócio: PurchaseOrder aggregate, 14 command handlers, domain services (interfaces), domain events |
| `realwave-sales-manager-infrastructure` | — | Lib | Utilitários transversais: LiquibaseHandler multi-tenant, error codes, extensões Jackson |
| `realwave-sales-manager-command-application` | **8080** | Spring Boot App | Recebe comandos HTTP, converte requests em Commands, despacha para handlers |
| `realwave-sales-manager-command-repository` | — | Lib | Implementação do repositório de escrita via Event Store (event-store-connector) |
| `realwave-sales-manager-query-application` | **8180** | Spring Boot App | Serve as queries REST, lê diretamente do Query DB (PostgreSQL via JDBC) |
| `realwave-sales-manager-query-repository` | — | Lib | Implementações JDBC, mappers ResultSet→Domain, migrations Liquibase |
| `realwave-sales-manager-query-event-handler` | — | Lib | 25 handlers que processam eventos do Event Store e atualizam o Query DB |
| `realwave-sales-manager-events` | — | Lib | DTOs Kafka: `PurchaseOrderChangeEvent` e utilitários de serialização |
| `realwave-sales-manager-producer` | — | Lib | `PurchaseOrderKafkaProducer` — publica estado do pedido no tópico Kafka |
| `realwave-sales-manager-consumer` | **8082** | Spring Boot App | `@KafkaListener` — consome eventos e dispara callbacks para sistemas externos |
| `realwave-sales-manager-integration` | — | Lib | 6 Feign clients para sistemas externos + wrappers de serviço + error decoders |

### 11.2 Command Handlers (14 handlers no módulo domain)

| Handler | Commands tratados |
|---|---|
| `PurchaseOrderCommandHandler` | Create, Find, Delete, Validate, UpdateType |
| `PurchaseOrderItemCommandHandler` | AddItem, RemoveItem, UpdateItem |
| `PurchaseOrderPaymentCommandHandler` | UpdatePayment |
| `PurchaseOrderFreightCommandHandler` | UpdateFreight |
| `PurchaseOrderCouponCommandHandler` | CreateCoupon, UpdateCoupon |
| `PurchaseOrderCustomerCommandHandler` | UpdateCustomer |
| `PurchaseOrderCustomerOrderCommandHandler` | Checkout, UpdateCustomerOrder |
| `PurchaseOrderMgmCommandHandler` | UpdateMgm, DeleteMgm |
| `PurchaseOrderSegmentationCommandHandler` | UpdateSegmentation |
| `PurchaseOrderOnBoardingSaleCommandHandler` | UpdateOnBoardingSale |
| `PurchaseOrderSalesForceCommandHandler` | UpdateSalesForce, RemoveSalesForce |
| `PurchaseOrderSubscriptionCommandHandler` | UpdateSubscription |
| `PurchaseOrderProtocolCommandHandler` | UpdateProtocol |
| `PurchaseOrderInstallationAttributesCommandHandler` | UpdateInstallationAttributes, DeleteInstallationAttributes |

---

## 12. GRAFO DE DEPENDÊNCIAS ENTRE MÓDULOS

```
                        ┌─────────────────────────────┐
                        │         pom.xml (root)       │
                        │  Gerencia versões e build    │
                        └──────────────┬──────────────┘
                                       │ parent
          ┌───────────────────────────┬┴──────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────┐      ┌────────────────────┐      ┌────────────────────┐
│ infrastructure  │      │       api           │      │      events        │
│ (utilitários)   │      │ (DTOs + interfaces) │      │ (DTOs Kafka)       │
└────────┬────────┘      └─────────┬──────────┘      └────────┬───────────┘
         │                         │                           │
         │                         ▼                           │
         │               ┌──────────────────┐                  │
         └──────────────►│     domain        │◄─────────────────┘
                         │ (PurchaseOrder +  │
                         │  Handlers)        │
                         └──────┬────────────┘
                                │
              ┌─────────────────┼──────────────────┐
              │                 │                  │
              ▼                 ▼                  ▼
┌─────────────────────┐  ┌────────────┐  ┌────────────────────┐
│ command-repository  │  │ integration│  │     producer        │
│ (Event Store)       │  │ (Feign CLI)│  │ (Kafka producer)    │
└──────────┬──────────┘  └─────┬──────┘  └──────────┬─────────┘
           │                   │                     │
           └──────────┬────────┘                     │
                      ▼                              │
          ┌───────────────────────┐                  │
          │  command-application  │◄─────────────────┘
          │  (Spring Boot :8080)  │
          └───────────────────────┘


┌──────────────────────────────────────────────────┐
│             EVENT STORE (externo)                │
│  Persiste todos os eventos imutavelmente         │
└──────────────────────────────────────────────────┘
          │ stream de eventos
          ▼
┌───────────────────────────┐
│  query-event-handler       │
│  (25 handlers)             │
└──────────────┬────────────┘
               │
               ▼
┌──────────────────────────────┐
│      query-repository        │
│  (JDBC + Liquibase migrations│
│   + mappers)                 │
└──────────────┬───────────────┘
               │ PostgreSQL
               ▼
┌──────────────────────────────┐
│    query-application         │
│    (Spring Boot :8180)        │
└──────────────────────────────┘


┌──────────────────────────────────────────┐
│   Kafka Topic: rw_sm_purchase_events     │
└──────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────┐
│       consumer               │
│  (Spring Boot :8082)          │
│  @KafkaListener               │
│  Group: sm-purchase-order-status│
│  → CallbackService            │
└──────────────────────────────┘
```

### Dependências por módulo

| Módulo | Depende de |
|---|---|
| `api` | — (apenas Spring Web, Validation) |
| `infrastructure` | — (utilitários puros) |
| `events` | — (DTOs Kafka) |
| `domain` | `api`, `infrastructure`, `events` |
| `command-repository` | `domain`, `event-store-connector` |
| `integration` | `domain`, `api`, `realwave-cms-client`, `realwave-pcm-client`, `realwave-cim-api`, `rw-coupon-api` |
| `producer` | `domain`, `events`, `spring-kafka` |
| `command-application` | `domain`, `command-repository`, `integration`, `producer` |
| `query-repository` | `domain`, `infrastructure`, `PostgreSQL`, `Liquibase` |
| `query-event-handler` | `domain`, `query-repository`, `event-store-connector` |
| `query-application` | `api`, `query-repository` |
| `consumer` | `domain`, `events`, `integration`, `spring-kafka` |

---

## 13. SEPARAÇÃO COMMAND/QUERY — COMO FUNCIONA

### 13.1 Command Side (Escrita — porta 8080)

**Fluxo de escrita:**
```
HTTP Request (write)
  → PurchaseOrderCommandController
  → Command object (imutável, carrega intenção)
  → Command Handler (lógica de negócio)
  → PurchaseOrder.método() (muta via evento)
  → applyChange(event) → lista de pendingEvents
  → Event.apply(this) → muta estado in-memory
  → repositoryManager.save(aggregate)
      └─ Event Store persiste eventos (append-only)
  → purchaseOrderProducer.notify(aggregate)
      └─ Kafka publica snapshot do estado
  → HTTP Response (201 com ID ou estado parcial)
```

**Recuperação do aggregate (Event Sourcing):**
```
repositoryManager.get(purchaseOrderId)
  → Event Store busca todos eventos do aggregate
  → Replay: aplica cada evento em ordem cronológica
  → Retorna PurchaseOrder reconstituído no estado atual
```

**Pontos importantes:**
- O banco de escrita é **append-only** — nunca há UPDATE ou DELETE no Event Store
- Cada operação de negócio gera 1 evento de domínio
- O aggregate é sempre reconstruído por replay antes de qualquer operação
- O Command Side **não** usa o Query DB — escrita e leitura são 100% separadas

### 13.2 Query Side (Leitura — porta 8180)

**Fluxo de leitura:**
```
HTTP Request (GET)
  → PurchaseOrderController (Query App)
  → JdbcPurchaseOrderRepository.find(id)
      └─ SELECT * FROM purchase_order WHERE id = ?
  → Lazy loading de tabelas relacionadas:
      ├─ ORDER_ITEM
      ├─ PAYMENT
      ├─ FREIGHT
      ├─ CUSTOMER_ORDER
      ├─ DISCOUNT
      └─ INSTALLATION_ATTRIBUTES
  → Mappers ResultSet → Domain Objects
  → HTTP Response com view completa
```

O Query DB é um modelo **desnormalizado** otimizado para leitura, com tabelas separadas por conceito e joins simples por `purchase_order_id`.

### 13.3 Sincronização (Event Store → Query DB)

```
┌─────────────────────────────────────────────────────┐
│ OPÇÃO A: Event Store → query-event-handler           │
│                                                      │
│ Event Store emite evento                             │
│   → PurchaseOrderEventHandlerImpl (listener)         │
│   → EventHandlerDiscoveryService                     │
│       └─ Descobre handler correto pelo tipo do evento│
│   → Handler específico (ex: ItemAddedEventHandler)  │
│   → JDBC INSERT/UPDATE no Query DB                   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ OPÇÃO B: Kafka → consumer → callback                 │
│                                                      │
│ Kafka (rw_sm_purchase_events)                        │
│   → PurchaseOrderConsumer (@KafkaListener)           │
│   → CallbackService.notify(purchaseOrder)            │
│   → HTTP POST para URL de callback externa           │
└─────────────────────────────────────────────────────┘
```

> **Nota:** O `consumer` (porta 8082) não atualiza o Query DB — apenas dispara callbacks. Quem atualiza o Query DB é o `query-event-handler`, conectado diretamente ao Event Store.

---

## 14. INTEGRAÇÕES EXTERNAS — DETALHAMENTO TÉCNICO

### 14.1 CustomerOrderManager (COM)

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Processar o pedido de compra e criar um CustomerOrder |
| **Quando chamado** | Durante o checkout (`POST /purchase-orders/{id}/checkout`) |
| **Protocolo** | HTTP via Feign — `CustomerOrderManagerApiService` |
| **Operação** | `POST /customer-orders` |
| **Request** | `CustomerOrderRequest` com: customerId, externalId (purchaseOrderId), produtos, ofertas, pagamento, frete, cupom, MGM, callback |
| **Response** | `CustomerOrderResponse` com: customerOrderId, status, steps[], boleto (se BOLETO) |
| **Callback reverso** | COM chama `POST /purchase-orders/callback` quando o status muda (PROCESSING → FINISHED/FAILED) |

**Payload de checkout enviado ao COM:**
```json
{
  "customerId": "cust-123",
  "externalId": "po-uuid",
  "callback": "http://sales-manager:8080/purchase-orders/callback",
  "products": [{ "productTypeId": "...", "installationAttributes": {...} }],
  "offers": [{
    "catalogOfferId": "offer-456",
    "catalogOfferType": "OFFER",
    "price": 100.00,
    "offerItems": [{ "catalogOfferItemId": "...", "price": 50.00 }],
    "pricesPerPeriod": [{ "startAt": 0, "endAt": 12, "totalPrice": 100.00 }]
  }],
  "payment": {
    "methods": [{ "method": "CREDIT_CARD", "methodId": "card-123", "price": 100.00, "installments": 3, "securityCode": "123" }]
  },
  "freight": { "type": "NORMAL", "price": 10.00, "address": {...} },
  "coupon": { "code": "PROMO10", "discounts": [...] },
  "mgm": { "invite": "mgm-code" }
}
```

### 14.2 CMS Client (Catalog/Offers)

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Buscar detalhes de ofertas do catálogo para montar o payload do COM |
| **Quando chamado** | No checkout, antes de chamar o COM (checkout normal sem cupom) |
| **Operação** | `GET /offers?ids=...` (busca em lote) |
| **Response** | `OfferRepresentation` com campos de oferta, produtos, composições |
| **Client** | `realwave-cms-client` (biblioteca Realwave) |

### 14.3 PCM Client (Product Composition)

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Buscar composição de produto para pedidos com cupom |
| **Quando chamado** | No checkout com cupom (`PurchaseOrderCouponCheckoutService`) |
| **Operação** | `GET /compositions/{catalogOfferItemId}` |
| **Response** | `CompositionRepresentation` |
| **Client** | `realwave-pcm-client` (biblioteca Realwave) |

### 14.4 CIM API / CustomerSearchApiService

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Validar se o cliente existe e está ativo |
| **Quando chamado** | Durante `validate()` antes do checkout |
| **Operações** | `GET /api/customers/{id}` e `GET /api/customers/{customerId}/products/{productId}` |
| **Response** | `CustomerSearchResponse` |
| **Client** | `realwave-cim-api` (biblioteca Realwave) |

### 14.5 Coupon API

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Validar se o cupom é válido para o cliente |
| **Quando chamado** | Durante `validate()` se cupom aplicado |
| **Operação** | `GET /v1/coupons/code/{code}/customer/{customerId}` |
| **Response** | `CouponRepresentation` |
| **Client** | `rw-coupon-api` (biblioteca Realwave) |

### 14.6 Member-Get-Member API

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Validar participação em promoção referral |
| **Quando chamado** | Durante `validate()` se MGM aplicado |
| **Operação** | `GET /v2/member/{memberGetMemberCode}/validate` |
| **Response** | Validação booleana |

### 14.7 Callback (sistema externo configurável)

| Aspecto | Detalhe |
|---|---|
| **Propósito** | Notificar sistema externo sobre mudança de status do pedido |
| **Quando chamado** | Quando Consumer Kafka recebe evento de mudança de status |
| **URL** | Dinâmica — configurada por pedido no campo `callback` |
| **Operação** | `POST {callback.url}` com headers customizados |
| **Client** | `PurchaseOrderCallbackApiService` (Feign dinâmico) |

### 14.8 Configuração Feign

```
SMFeignConfig:
  connect-timeout: 1000ms
  read-timeout:    1000ms
  Interceptor:     SalesManagerFeignInterceptor (injeta headers de tenant)
  Auth EventStore: EventStoreBasicAuth (Basic Auth para Event Store API)
  Error Decoders:  Um por serviço externo
```

---

## 15. ESQUEMA DO BANCO DE DADOS (Query DB)

PostgreSQL com migrations Liquibase. Esquema **multi-tenant** (schema por tenant, gerenciado pelo `LiquibaseHandler`).

### Tabela principal: `purchase_order`

```sql
CREATE TABLE purchase_order (
  id                        VARCHAR(36)   PRIMARY KEY,
  customer                  VARCHAR(36),
  status                    TEXT          NOT NULL,  -- OPENED, CHECKED_OUT, FINISHED, CANCELLED
  segmentation              JSONB,
  mgm_code                  VARCHAR(36),
  mgm_custom_fields         JSONB,
  on_boarding_sale_offer_id VARCHAR(36),
  on_boarding_sale_custom_fields JSONB,
  coupon_code               VARCHAR(36),
  coupon_custom_fields      JSONB,
  payment_description       TEXT,
  purchase_order_type       TEXT,                    -- NORMAL, COUPON, CUSTOM_PLAN
  subscription_id           TEXT,
  protocol                  VARCHAR(36)   UNIQUE,
  channel_create            VARCHAR(50),
  channel_checkout          VARCHAR(50),
  callback                  TEXT,
  callback_headers          JSONB,
  reason                    JSONB,
  sales_force_id            VARCHAR(36),
  sales_force_name          VARCHAR(255),
  created                   TIMESTAMP     NOT NULL,
  updated                   TIMESTAMP,
  version                   INTEGER
);
```

### Tabelas relacionadas (todas têm FK para `purchase_order.id`)

```sql
-- Itens do pedido
CREATE TABLE order_item (
  id                  UUID          PRIMARY KEY,
  purchase_order_id   VARCHAR(36)   REFERENCES purchase_order(id),
  catalog_offer_id    VARCHAR(36),
  catalog_offer_type  VARCHAR(36),
  price_amount        DECIMAL,
  price_currency      VARCHAR(3),
  product_id          VARCHAR(36),
  quantity            INTEGER,
  validity_start_at   INTEGER,
  validity_end_at     INTEGER,
  offer_items         JSONB          -- composições e sub-itens
);

-- Métodos de pagamento
CREATE TABLE payment (
  id                      UUID        PRIMARY KEY,
  purchase_order_id       VARCHAR(36) REFERENCES purchase_order(id),
  method                  VARCHAR(50),   -- BOLETO, CREDIT_CARD, DEBIT_CARD, REWARD, etc.
  method_id               VARCHAR(255),
  price_amount            DECIMAL,
  price_currency          VARCHAR(3),
  custom_fields           JSONB,
  security_code_informed  BOOLEAN,
  installments            INTEGER,
  version                 INTEGER
);

-- Dados de frete/entrega
CREATE TABLE freight (
  id                  UUID        PRIMARY KEY,
  purchase_order_id   VARCHAR(36) REFERENCES purchase_order(id),
  type                VARCHAR(50),
  price_amount        DECIMAL,
  price_currency      VARCHAR(3),
  city                VARCHAR(255),
  state               VARCHAR(50),
  street              TEXT,
  number              VARCHAR(50),
  complement          TEXT,
  district            TEXT,
  country             VARCHAR(50),
  zip_code            VARCHAR(20),
  delivery_total_time INTEGER,       -- dias
  version             INTEGER
);

-- CustomerOrder (resultado do checkout no COM)
CREATE TABLE customer_order (
  id                  UUID        PRIMARY KEY,
  purchase_order_id   VARCHAR(36) REFERENCES purchase_order(id),
  customer_order_id   VARCHAR(255),
  status              TEXT,
  steps               JSONB,         -- workflow steps [{step, status, startedAt, endedAt}]
  boleto              JSONB,         -- {url, barcode, expirationDate} se BOLETO
  version             INTEGER
);

-- Descontos de cupom
CREATE TABLE discount (
  id                       UUID    PRIMARY KEY,
  purchase_order_id        VARCHAR(36) REFERENCES purchase_order(id),
  coupon_code              VARCHAR(36),
  discount_price_amount    DECIMAL,
  discount_price_currency  VARCHAR(3),
  discount_description     TEXT,
  segment_name             TEXT,
  percent                  DECIMAL
);

-- Atributos de instalação por tipo de produto
CREATE TABLE installation_attributes (
  id                  UUID        PRIMARY KEY,
  purchase_order_id   VARCHAR(36) REFERENCES purchase_order(id),
  product_type_id     VARCHAR(36),
  attributes          JSONB,
  version             INTEGER
);
```

---

## 16. FLUXO DE DADOS COMPLETO — DO INÍCIO AO FIM

```
╔══════════════════════════════════════════════════════════════════╗
║  FASE 1 — CRIAR PEDIDO                                          ║
╚══════════════════════════════════════════════════════════════════╝

  POST /purchase-orders
  { "type": "NORMAL", "customer": {"id": "cust-123"}, "callback": "https://..." }
       │
       ▼
  PurchaseOrderCommandController.create(request)
       │  converte para
       ▼
  CreatePurchaseOrderCommand(id=UUID, type, customer, callback)
       │
       ▼
  PurchaseOrderCommandHandler.handle(cmd)
       │  instancia aggregate
       ▼
  new PurchaseOrder()
       │  applyChange()
       ▼
  PurchaseOrderCreated event → event.apply(this) → status = OPENED
       │
       ├──► Event Store.save(aggregate)   [append-only]
       │
       └──► KafkaProducer.send("rw_sm_purchase_events", snapshot)
                                                  │
                                                  ▼
                                    PurchaseOrderConsumer (async)
                                    → PurchaseOrderCreatedEventHandler
                                    → INSERT INTO purchase_order (...)

  Response: 201 { "id": "po-uuid-123" }


╔══════════════════════════════════════════════════════════════════╗
║  FASE 2 — MONTAR PEDIDO (operações repetíveis)                  ║
╚══════════════════════════════════════════════════════════════════╝

  POST /purchase-orders/{id}/items          → ItemAdded event      → UPDATE order_item
  POST /purchase-orders/{id}/payment        → PaymentUpdated event → UPDATE payment
  POST /purchase-orders/{id}/freight        → FreightUpdated event → UPDATE freight
  POST /purchase-orders/{id}/coupon         → CouponUpdated event  → UPDATE purchase_order.coupon_code
  POST /purchase-orders/{id}/customer       → CustomerUpdated      → UPDATE purchase_order.customer
  POST /purchase-orders/{id}/segmentation   → SegmentationUpdated  → UPDATE purchase_order.segmentation
  POST /purchase-orders/{id}/sales-force    → SalesForceUpdated    → UPDATE purchase_order.sales_force_*
  POST /purchase-orders/{id}/mgm            → MgmUpdated           → UPDATE purchase_order.mgm_*

  Cada operação segue o mesmo padrão:
  Controller → Command → Handler → aggregate.método()
           → Event → Event Store → Kafka → Query Event Handler → Query DB


╔══════════════════════════════════════════════════════════════════╗
║  FASE 3 — VALIDAR                                               ║
╚══════════════════════════════════════════════════════════════════╝

  POST /purchase-orders/{id}/validate
       │
       ▼
  PurchaseOrderCommandHandler.handle(ValidatePurchaseOrder)
       │  reconstrói aggregate por replay do Event Store
       ▼
  PurchaseOrderValidator.validate(purchaseOrder)
       ├── customer != null ?                           [domain rule]
       ├── items.size > 0 ?                             [domain rule]
       ├── payment.methods != empty ?                  [domain rule]
       ├── CustomerInfoService.isActive(customerId) ?  [→ CIM API]
       ├── CatalogManagerService.validate(items) ?     [→ CMS/PCM]
       ├── CouponService.validate(coupon) ?             [→ Coupon API, se houver]
       └── MemberGetMemberService.validate(mgm) ?      [→ MGM API, se houver]

  OK   → 201 {}
  FAIL → 400 { "errors": [...] }


╔══════════════════════════════════════════════════════════════════╗
║  FASE 4 — CHECKOUT                                              ║
╚══════════════════════════════════════════════════════════════════╝

  POST /purchase-orders/{id}/checkout
  { "paymentSecurityCodes": [{ "methodId": "card-123", "securityCode": "123" }] }
       │
       ▼
  CheckoutCommand(purchaseOrderId, channel, securityCodes)
       │
       ▼
  PurchaseOrderCustomerOrderCommandHandler
       │  replay Event Store → PurchaseOrder
       │  re-valida
       ▼
  CheckoutStrategy.resolve(type):
       │
       ├── [sem cupom] PurchaseOrderCheckoutService
       │     └─► CMS Client: GET /offers?ids=...  → OfferRepresentation
       │
       └── [com cupom] PurchaseOrderCouponCheckoutService
             └─► PCM Client: GET /compositions/{id}  → CompositionRepresentation
       │
       ▼
  CustomerOrderManagerApiService (Feign)
  POST http://com-service/customer-orders
  { customerId, externalId, produtos, ofertas, pagamento, frete, cupom, mgm }
       │
       ▼
  CustomerOrderResponse { id: "com-order-uuid", status: "PROCESSING", steps: [...] }
       │
       ▼
  purchaseOrder.checkout(customerOrder, channel, securityCodes)
       │  applyChange()
       ▼
  PurchaseOrderCheckedOut event → status = CHECKED_OUT
       │
       ├──► Event Store.save()
       │
       └──► Kafka: PurchaseOrderCheckedOut
                        │
                        ├─► query-event-handler:
                        │     PurchaseOrderCheckedOutEventHandler
                        │     → UPDATE purchase_order SET status = CHECKED_OUT
                        │     → INSERT INTO customer_order (id, status, steps)
                        │
                        └─► consumer:
                              CallbackService (se callback != null)
                              → POST {callback.url} { id, status: CHECKED_OUT }

  Response: 201 {
    "purchaseOrderId": "po-uuid-123",
    "customerOrder": { "id": "com-order-uuid", "boleto": {...} }
  }


╔══════════════════════════════════════════════════════════════════╗
║  FASE 5 — CALLBACK DO COM (assíncrono)                          ║
╚══════════════════════════════════════════════════════════════════╝

  COM → POST /purchase-orders/callback
  { "id": "com-order-uuid", "externalId": "po-uuid-123", "status": "FINISHED", "steps": [...] }
       │
       ▼
  UpdateCustomerOrderCommand(purchaseOrderId, customerOrder, reason?)
       │
       ▼
  PurchaseOrderCustomerOrderCommandHandler
       │  replay → PurchaseOrder
       ▼
  purchaseOrder.updateCustomerOrder(customerOrder, reason)
       │  applyChange()
       ▼
  PurchaseOrderCustomerOrderUpdated event
  PurchaseOrderStatusUpdated event (se status mudou)
       │
       ├──► Event Store.save()
       │
       └──► Kafka: eventos
                        │
                        ├─► query-event-handler:
                        │     → UPDATE customer_order SET status = FINISHED
                        │     → UPDATE purchase_order SET status = COMPLETED/FAILED
                        │
                        └─► consumer:
                              CallbackService
                              → POST {callback.url} { id, status: COMPLETED }
                              (notifica sistema que originou o pedido)


╔══════════════════════════════════════════════════════════════════╗
║  FASE 6 — CONSULTA                                              ║
╚══════════════════════════════════════════════════════════════════╝

  GET /purchase-orders/po-uuid-123     (porta 8180)
       │
       ▼
  PurchaseOrderController (Query App)
       │
       ▼
  JdbcPurchaseOrderRepository.find(id)
       ├── SELECT * FROM purchase_order WHERE id = ?
       ├── SELECT * FROM order_item WHERE purchase_order_id = ?
       ├── SELECT * FROM payment WHERE purchase_order_id = ?
       ├── SELECT * FROM freight WHERE purchase_order_id = ?
       ├── SELECT * FROM customer_order WHERE purchase_order_id = ?
       ├── SELECT * FROM discount WHERE purchase_order_id = ?
       └── SELECT * FROM installation_attributes WHERE purchase_order_id = ?
       │
       ▼
  Mappers (ResultSet → Domain Objects)
  ItemMapper, PaymentMapper, FreightMapper, CustomerOrderMapper, etc.
       │
       ▼
  Response 200: PurchaseOrderResponse completo
  {
    "id": "po-uuid-123",
    "customer": "cust-123",
    "status": "COMPLETED",
    "type": "NORMAL",
    "items": [...],
    "payment": { "methods": [...] },
    "freight": { "type": "NORMAL", "price": {...}, "address": {...} },
    "customerOrder": { "id": "com-order-uuid", "status": "FINISHED", "steps": [...] },
    "coupon": { "code": "PROMO10", "reward": { "discounts": [...] } },
    "createdAt": "2026-03-30T10:00:00Z",
    "updatedAt": "2026-03-30T11:30:00Z"
  }


╔══════════════════════════════════════════════════════════════════╗
║  VISÃO GERAL DO CICLO COMPLETO                                  ║
╚══════════════════════════════════════════════════════════════════╝

  [CLIENTE/CANAL]
       │
       │ POST /purchase-orders → 201 { id }
       │ POST /{id}/items      → 201 { itemId }
       │ POST /{id}/payment    → 201 { id }
       │ POST /{id}/validate   → 201 {}
       │ POST /{id}/checkout   → 201 { customerOrderId, boleto? }
       │ GET  /{id}            → 200 { status: CHECKED_OUT }
       ▼
  [Async: COM processa pagamento]
       ▼
  POST /purchase-orders/callback → status: COMPLETED ou FAILED
       ▼
  [Async: notificação para callback externo]
       ▼
  GET /{id}  → 200 { status: COMPLETED }

  Estados finais possíveis: COMPLETED | FAILED | CANCELED | DELETED
```

---

## 17. DEPENDÊNCIAS PROPRIETÁRIAS — MAPEAMENTO PARA SPRING BOOT 3.x

Todas as dependências abaixo são artefatos internos da Zup/Realwave e **não existem em repositórios públicos**. Precisam ser reescritas ou substituídas por equivalentes open-source ao migrar para Spring Boot 3.x.

### 17.1 Núcleo de Event Sourcing (3 artefatos)

#### `br.com.zup:event-sourcing-core` · v1.5.0

| | |
|---|---|
| **O que faz** | Framework proprietário de Event Sourcing. Fornece a interface `AggregateRoot`, o mecanismo `applyChange(event)`, replay de eventos e a abstração `RepositoryManager` que reconstrói aggregates a partir do Event Store. É a base de toda a lógica de domínio. |
| **Por que substituir** | Biblioteca interna sem suporte público. Incompatível com Java 17+ e Spring Boot 3.x. |
| **Substituto moderno** | **Axon Framework 4.x** (`org.axonframework:axon-spring-boot-starter`) — CQRS+ES open-source com suporte a Spring Boot 3.x, conceitos idênticos (AggregateRoot, @CommandHandler, @EventSourcingHandler). Alternativa: implementação própria com `spring-data-jpa` + tabela de eventos (UUID, aggregate_id, type, payload JSONB, version, created_at). |

#### `br.com.zup:event-store-connector` · v1.5.0

| | |
|---|---|
| **O que faz** | Conector HTTP entre o `event-sourcing-core` e o servidor EventStore.org. Serializa/desserializa eventos, gerencia streams por aggregate ID e implementa `PurchaseOrderEventHandlerImpl` para projeções. |
| **Por que substituir** | Acoplado ao EventStore.org via protocolo proprietário e à biblioteca interna. |
| **Substituto moderno** | **Axon Server** (Community Edition) ou PostgreSQL com tabela `domain_events` append-only gerenciada pelo próprio código. |

#### `br.com.zup:relational-database-connector` · v1.5.0

| | |
|---|---|
| **O que faz** | Variante do `event-store-connector` que persiste eventos em PostgreSQL em vez do EventStore.org. |
| **Por que substituir** | Biblioteca interna sem suporte. A lógica é simples o suficiente para ser reimplementada. |
| **Substituto moderno** | Tabela `domain_events` simples gerenciada por `spring-data-jpa` ou `spring-jdbc`. |

### 17.2 Multi-tenancy

#### `br.com.zup:spring-tenant` · v1.1.3

| | |
|---|---|
| **O que faz** | Gerencia contexto multi-tenant por requisição. Extrai o tenant dos headers HTTP, popula um `ThreadLocal` (`TenantContext`), e integra com Liquibase para criação de schemas isolados por tenant. |
| **Por que substituir** | Biblioteca interna. |
| **Substituto moderno** | `TenantFilter` customizado + `AbstractRoutingDataSource` do Spring para roteamento de DataSource por tenant + `MultiTenantSpringLiquibase` para migrations por schema. |

### 17.3 Contexto Web / Request Propagation

#### `br.com.zup.realwave:realwave-context-web` · v2020R4.5.0

| | |
|---|---|
| **O que faz** | Popula e propaga o `RealwaveContextHolder` (ThreadLocal) com dados da requisição HTTP: tenant, canal (WEB/MOBILE), usuário autenticado, correlation ID. Injeta esses headers nas chamadas Feign outbound. |
| **Por que substituir** | Depende de APIs internas do Realwave. |
| **Substituto moderno** | **Micrometer Tracing** para correlation ID. Para dados customizados de tenant/canal: `OncePerRequestFilter` + `MDC` do SLF4J + interceptor Feign/RestClient customizado. |

### 17.4 Tratamento de Erros

#### `br.com.zup.realwave:realwave-exception-handler` · v2020R4.5.0

| | |
|---|---|
| **O que faz** | `@ControllerAdvice` pré-configurado que intercepta exceções e as converte para respostas HTTP padronizadas no formato Realwave (`{errors: [{code, message}]}`). Registra no Graylog automaticamente. |
| **Por que substituir** | Acoplado ao formato de erro interno e ao Graylog. |
| **Substituto moderno** | `@RestControllerAdvice` nativo + `ProblemDetail` (RFC 7807 — `application/problem+json`). Suporte nativo no Spring MVC 6+. |

### 17.5 Observabilidade / Logging

#### `br.com.zup.realwave.common:realwave-graylog` · v1.1.6

| | |
|---|---|
| **O que faz** | Auto-configuração de Log4j2 para enviar logs estruturados ao Graylog via GELF. Configura appenders, formatação JSON e campos de contexto automaticamente. |
| **Por que substituir** | Log4j2 em versão desatualizada. Ecossistema moderno favorece OpenTelemetry. |
| **Substituto moderno** | **Logback** + `logstash-logback-encoder` para JSON estruturado. Para traces: **OpenTelemetry** (`io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter`). |

### 17.6 Serialização / Deserialização Kotlin

#### `br.com.zup.realwave:spring-boot-starter-realwave-kserialize` · v2018R1.0.2
#### `br.com.zup.realwave:spring-boot-starter-realwave-serialize` · v1.0.1

| | |
|---|---|
| **O que fazem** | Configuram o ObjectMapper Jackson para suportar tipos Kotlin: `KotlinModule` para data classes, serializadores customizados para value objects do domínio e configuração de null-safety. |
| **Por que substituir** | Wrappers internos sobre Jackson. O suporte Kotlin é hoje público e maduro. |
| **Substituto moderno** | `com.fasterxml.jackson.module:jackson-module-kotlin` (incluído automaticamente pelo `spring-boot-starter-web` no Spring Boot 3.x com Kotlin). |

### 17.7 Feign Commons

#### `br.com.zup.realwave:spring-boot-starter-realwave-feign-commons` · v2019R2.0.0

| | |
|---|---|
| **O que faz** | Auto-configuração de clientes Feign: propaga headers de autenticação (JWT), injeta tenant e correlation ID nas chamadas outbound, configura encoders/decoders Jackson com suporte Kotlin, e fornece `ErrorDecoder` padrão. |
| **Por que substituir** | Acoplado à infraestrutura Realwave. OpenFeign 9.x incompatível com Spring Boot 3.x. |
| **Substituto moderno** | **Spring Cloud OpenFeign** (versão compatível com Spring Boot 3.x) ou **`RestClient`** nativo do Spring 6. Propagação de headers via `RequestInterceptor` customizado. |

### 17.8 Autenticação / IAM

#### `br.com.zup:spring-boot-starter-zup-iam` · v2018R1.0.2
#### `br.com.zup:zup-iam-adapters` · v2018R1.0.2

| | |
|---|---|
| **O que fazem** | Integração com IAM interno (Keycloak). Configura filtros Spring Security que validam tokens JWT, extrai claims (usuário, roles, tenant) e popula o `SecurityContext`. |
| **Por que substituir** | Wrappers sobre o `keycloak-spring-boot-adapter`, **descontinuado pela Red Hat em 2022**. Incompatível com Spring Security 6. |
| **Substituto moderno** | **Spring Security OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`), nativo no Spring Boot 3.x. Configuração: `spring.security.oauth2.resourceserver.jwt.issuer-uri`. |

### 17.9 Clientes de Sistemas Internos Realwave

Estes artefatos são SDKs cliente de outros serviços. No rebuild, substituídos por clientes HTTP diretos.

#### `br.com.zup.realwave:realwave-cms-client` · v2018R2.4.0
- **O que faz:** Cliente Feign para o CMS. `CmsOfferClient.offers(ids)` → `OfferRepresentation` (detalhes de ofertas para o checkout).
- **Substituto:** Novo `@FeignClient` ou `RestClient` para a API do CMS com DTO local `OfferResponse`.

#### `br.com.zup.realwave:realwave-pcm-client` · v2018R1.0.22
- **O que faz:** Cliente Feign para o PCM. `PcmCompositionClient.findOne(catalogOfferItemId)` → `CompositionRepresentation` (usado no checkout com cupom).
- **Substituto:** Novo `@FeignClient` ou `RestClient` para a API do PCM.

#### `br.com.zup.realwave:realwave-cim-api` · v3.0.1
- **O que faz:** Cliente Feign para o CIM. `CustomerSearchApiService.findById(customerId)` — valida cliente na etapa de validação.
- **Substituto:** Novo `@FeignClient` ou `RestClient` para a API do CIM.

#### `br.com.zup.rw:rw-coupon-api` · v2017R4.3.0
- **O que faz:** Cliente Feign para o Coupon Service. `CouponApiService.validationCoupon(code, customerId)`.
- **Substituto:** Novo `@FeignClient` ou `RestClient` para o Coupon Service.

### 17.10 Tabela Resumo — Plano de Migração

| Dependência proprietária | Versão | Categoria | Substituto Spring Boot 3.x |
|---|---|---|---|
| `event-sourcing-core` | 1.5.0 | Framework ES | Axon Framework 4.x **ou** implementação própria com `spring-data-jpa` |
| `event-store-connector` | 1.5.0 | Persistência ES | Axon Server **ou** tabela `domain_events` com JDBC |
| `relational-database-connector` | 1.5.0 | Persistência ES | `spring-data-jpa` + tabela de eventos |
| `spring-tenant` | 1.1.3 | Multi-tenancy | `AbstractRoutingDataSource` + `TenantFilter` customizado |
| `realwave-context-web` | 2020R4.5.0 | Request Context | `OncePerRequestFilter` + `MDC` + Micrometer Tracing |
| `realwave-exception-handler` | 2020R4.5.0 | Error Handling | `@RestControllerAdvice` + `ProblemDetail` (RFC 7807) |
| `realwave-graylog` | 1.1.6 | Logging | Logback + `logstash-logback-encoder` + OpenTelemetry |
| `spring-boot-starter-realwave-kserialize` | 2018R1.0.2 | Serialização | `jackson-module-kotlin` (incluído no Spring Boot 3) |
| `spring-boot-starter-realwave-serialize` | 1.0.1 | Serialização | `jackson-module-kotlin` |
| `spring-boot-starter-realwave-feign-commons` | 2019R2.0.0 | HTTP Client | Spring Cloud OpenFeign **ou** `RestClient` nativo |
| `spring-boot-starter-zup-iam` | 2018R1.0.2 | Autenticação | `spring-boot-starter-oauth2-resource-server` |
| `zup-iam-adapters` | 2018R1.0.2 | Autenticação | `spring-boot-starter-oauth2-resource-server` |
| `realwave-cms-client` | 2018R2.4.0 | Cliente HTTP | Novo `@FeignClient`/`RestClient` para CMS |
| `realwave-pcm-client` | 2018R1.0.22 | Cliente HTTP | Novo `@FeignClient`/`RestClient` para PCM |
| `realwave-cim-api` | 3.0.1 | Cliente HTTP | Novo `@FeignClient`/`RestClient` para CIM |
| `rw-coupon-api` | 2017R4.3.0 | Cliente HTTP | Novo `@FeignClient`/`RestClient` para Coupon Service |
| `rw-event` | 2.0.0 | Eventos Kafka | DTOs locais + `spring-kafka` puro |

### Dependências de maior impacto no rebuild

1. **`event-sourcing-core` + `event-store-connector`** — risco alto. São a base de toda a arquitetura. A decisão entre Axon Framework vs. implementação própria define o design do novo sistema.
2. **`spring-boot-starter-zup-iam`** — risco médio. Exige configurar o Keycloak corretamente com Spring Security 6, mas o substituto é bem documentado.
3. **`realwave-context-web`** — risco médio. A propagação de tenant/canal permeia todos os módulos; precisa ser reimplementada de forma consistente.
4. **SDKs cliente** (cms, pcm, cim, coupon) — risco baixo. São apenas clientes HTTP; basta criar novos `@FeignClient` com os DTOs necessários.

---

## 21. ANÁLISE DETALHADA DO AGGREGATE: PurchaseOrder

> Baseado na leitura direta do código-fonte. Esta seção é a referência definitiva para o rebuild.

---

### 21.1 Estados Possíveis (`PurchaseOrderStatus`)

```kotlin
enum class PurchaseOrderStatus {
    OPENED,
    CHECKED_OUT,
    COMPLETED,
    FAILED,    // definido no enum mas NUNCA atribuído pelo código atual (ver nota abaixo)
    CANCELED,
    DELETED
}
```

> **Nota importante:** O status `FAILED` existe no enum mas **não é atribuído** por nenhum caminho de código descoberto. Os erros de pagamento retornam o pedido para `OPENED` (ver transição 5 abaixo).

---

### 21.2 Máquina de Estados — Transições

```
                    ┌─────────────────────────────────────────────────────┐
                    │              PURCHASE ORDER                         │
                    │                                                     │
  create()          │              ┌──────────┐                          │
──────────────────► │   OPENED ───►│CHECKED_OUT├───► COMPLETED           │
                    │      │       └──────────┘  │                       │
                    │      │          ▲           ├───► CANCELED          │
                    │      │          │           │                       │
                    │      │       PARTIAL /      └───► OPENED  (rollback)│
                    │      │       REJECTED /                             │
                    │      │       FAILED                                 │
                    │      │   (COM callback)                             │
                    │      │                                              │
                    │      ▼                                              │
                    │   DELETED                                           │
                    └─────────────────────────────────────────────────────┘
```

#### Transição 1: `→ OPENED` (criação)
- **Trigger:** `CreatePurchaseOrderCommand` ou `CreatePurchaseOrderCouponCommand`
- **Evento emitido:** `PurchaseOrderCreated`
- **Pré-condição:** nenhuma (é a criação)
- **Kafka:** `notifyPurchaseOrderStateUpdated()` → eventType `"PurchaseOrderCreated"`

#### Transição 2: `OPENED → CHECKED_OUT` (checkout)
- **Trigger:** `CheckoutCommand`
- **Evento emitido:** `PurchaseOrderCheckedOut`
- **Pré-condições verificadas em sequência:**
  1. `verifyPurchaseOrderIsOpen()` — status deve ser OPENED
  2. `purchaseOrderValidator.validate()` — validações por tipo (ver 21.5)
  3. `canCheckout()` — customer, payment, items, sem métodos duplicados
- **Kafka:** eventType `"PurchaseOrderCheckedout"`

#### Transição 3: `CHECKED_OUT → COMPLETED`
- **Trigger:** `UpdateCustomerOrderCommand` com `customerOrder.status == "COMPLETED"`
- **Eventos emitidos:** `PurchaseOrderCustomerOrderUpdated` + `PurchaseOrderStatusUpdated(COMPLETED)`
- **Pré-condição:** callback do COM com status COMPLETED
- **Kafka:** `notifyPurchaseOrderStateUpdated()` → eventType `"PurchaseOrderFinished"`

#### Transição 4: `CHECKED_OUT → CANCELED`
- **Trigger:** `UpdateCustomerOrderCommand` com `customerOrder.status == "CANCELED"`
- **Eventos emitidos:** `PurchaseOrderCustomerOrderUpdated` + `PurchaseOrderStatusUpdated(CANCELED)`
- **Kafka:** eventType `"PurchaseOrderFinished"`

#### Transição 5: `CHECKED_OUT → OPENED` (rollback — comportamento crítico\!)
- **Trigger:** `UpdateCustomerOrderCommand` com `customerOrder.status` em `["PARTIAL", "REJECTED", "FAILED"]`
- **Eventos emitidos:** `PurchaseOrderCustomerOrderUpdated` + `PurchaseOrderStatusUpdated(OPENED)`
- **Nota:** O pedido **volta para OPENED**, não vai para FAILED. Isso permite nova tentativa de checkout.
- **Kafka:** eventType `"PurchaseOrderFinished"`

#### Transição 6: `OPENED → DELETED`
- **Trigger:** `DeletePurchaseOrderCommand`
- **Evento emitido:** `PurchaseOrderDeleted`
- **Pré-condição:** `canDelete()` — status deve ser OPENED
- **Erro se não OPENED:** `PURCHASE_ORDER_CANNOT_DELETE`

---

### 21.3 Tipos de Pedido (`PurchaseOrderType`)

```kotlin
enum class PurchaseOrderType {
    JOIN,    // Novo cliente contratando pela primeira vez
    CHANGE,  // Alteração de plano de cliente existente (requer subscriptionId)
    BUY,     // Compra avulsa (requer productId em todos os itens)
    COUPON   // Compra com cupom de desconto (usa fluxo PCM no checkout)
}
```

**Regra importante:** Uma vez definido, o `type` **não pode ser redefinido**. `verifyPurchaseOrderType()` lança `PURCHASE_ORDER_TYPE_IS_ALREADY_DEFINED` se `type \!= null`.

---

### 21.4 Todos os Commands com Campos

| Command | Campos | Handler |
|---------|--------|---------|
| `CreatePurchaseOrderCommand` | `id: PurchaseOrderId`, `purchaseOrderType: PurchaseOrderType?`, `callback: Callback?`, `customer: Customer?` | PurchaseOrderCommandHandler |
| `CreatePurchaseOrderCouponCommand` | `id: PurchaseOrderId`, `couponCode: String`, `productId: String`, `customerId: String`, `callback: Callback?` | PurchaseOrderCouponCommandHandler |
| `FindPurchaseOrderCommand` | `id: PurchaseOrderId` | PurchaseOrderCommandHandler |
| `DeletePurchaseOrderCommand` | `id: PurchaseOrderId` | PurchaseOrderCommandHandler |
| `ValidatePurchaseOrder` | `id: PurchaseOrderId` | PurchaseOrderCommandHandler |
| `UpdatePurchaseOrderType` | `id: PurchaseOrderId`, `type: PurchaseOrderType?` | PurchaseOrderCommandHandler |
| `CheckoutCommand` | `id: PurchaseOrderId`, `channel: Channel`, `securityCodes: List<SecurityCode>` | PurchaseOrderCustomerOrderCommandHandler |
| `UpdateCustomerOrderCommand` | `id: PurchaseOrderId`, `customerOrder: CustomerOrder`, `reason: Reason?` | PurchaseOrderCustomerOrderCommandHandler |
| `AddItemCommand` | `id: PurchaseOrderId`, `item: Item` | PurchaseOrderItemCommandHandler |
| `UpdateItemCommand` | `id: PurchaseOrderId`, `item: Item` | PurchaseOrderItemCommandHandler |
| `RemoveItemCommand` | `id: PurchaseOrderId`, `itemId: Item.Id` | PurchaseOrderItemCommandHandler |
| `UpdatePaymentCommand` | `id: PurchaseOrderId`, `payment: Payment` | PurchaseOrderPaymentCommandHandler |
| `UpdateFreightCommand` | `id: PurchaseOrderId`, `freight: Freight` | PurchaseOrderFreightCommandHandler |
| `UpdateCouponCommand` | `id: PurchaseOrderId`, `couponCode: CouponCode` | PurchaseOrderCouponCommandHandler |
| `UpdateCustomerCommand` | `id: PurchaseOrderId`, `customer: Customer` | PurchaseOrderCustomerCommandHandler |
| `UpdateMgmCommand` | `id: PurchaseOrderId`, `mgm: Mgm` | PurchaseOrderMgmCommandHandler |
| `DeleteMgmCommand` | `id: PurchaseOrderId` | PurchaseOrderMgmCommandHandler |
| `UpdateSegmentationCommand` | `id: PurchaseOrderId`, `segmentation: Segmentation` | PurchaseOrderSegmentationCommandHandler |
| `UpdateOnBoardingSaleCommand` | `id: PurchaseOrderId`, `onBoardingSale: OnBoardingSale` | PurchaseOrderOnBoardingSaleCommandHandler |
| `UpdateSalesForceCommand` | `id: PurchaseOrderId`, `salesForce: SalesForce` | PurchaseOrderSalesForceCommandHandler |
| `RemoveSalesForceCommand` | `id: PurchaseOrderId` | PurchaseOrderSalesForceCommandHandler |
| `UpdateProtocolCommand` | `id: PurchaseOrderId`, `protocol: Protocol` | PurchaseOrderProtocolCommandHandler |
| `UpdateSubscriptionCommand` | `id: PurchaseOrderId`, `subscription: Subscription` | PurchaseOrderSubscriptionCommandHandler |
| `UpdateInstallationAttributesCommand` | `id: PurchaseOrderId`, `installationAttribute: InstallationAttribute` | PurchaseOrderInstallationAttributesCommandHandler |
| `DeleteInstallationAttributesCommand` | `id: PurchaseOrderId`, `productTypeId: ProductTypeId` | PurchaseOrderInstallationAttributesCommandHandler |

---

### 21.5 Validações — Mapa Completo

#### Validações estruturais (domain, antes de `applyChange`)

Estas funções são chamadas por extension functions e lançam exceções **antes** de emitir eventos:

| Função | Onde é chamada | O que verifica | Exceção se falhar |
|--------|---------------|----------------|-------------------|
| `verifyPurchaseOrderIsOpen()` | Quase toda mutation | `status == OPENED` | `BusinessException(PURCHASE_ORDER_NOT_EXISTS)` |
| `verifyPurchaseOrderType()` | `updatePurchaseOrderType()` | `type == null` | `BusinessException(PURCHASE_ORDER_TYPE_IS_ALREADY_DEFINED)` |
| `canDelete()` | `deletePurchaseOrder()` | `status == OPENED` | `BusinessException(PURCHASE_ORDER_CANNOT_DELETE)` |
| `hasCustomer()` | `canCheckout()`, `updateCoupon()` | `customer \!= null` | `BusinessException(CUSTOMER_NOT_INFORMED)` |
| `canCheckout()` | `CheckoutCommand.execute()` | customer + payment não vazio + items não vazio + sem métodos duplicados | `BusinessException(PAYMENT_NOT_FOUND)`, `BusinessException(ITEMS_NOT_INFORMED)`, `PurchaseOrderValidationMethodsException(MORE_ONE_PAYMENT_METHOD)` |
| `purchaseOrderHasItem(itemId)` | `removeItem()`, `updateItem()` | item existe em `items` | `NotFoundException(CatalogOfferId)` |
| `verifyInstallationAttributesConstainsProductTypeId(ptid)` | delete installation attrs | productTypeId existe no map | `NotFoundException(productTypeId)` |
| `validatePurchaseOrderTypeForCoupon()` | `updateCoupon()` | `type in [BUY, JOIN]` | `BusinessException(PURCHASE_ORDER_INVALID_TYPE)` |

#### Validações de negócio (`PurchaseOrderValidationService.validate()`)

Chamada em `ValidatePurchaseOrder` e no início do `CheckoutCommand.execute()`:

```
validate(purchaseOrder):
  1. validateCustomer()         → se customer \!= null: customerInfoService.validateCustomer(customer.id)
  2. switch (type):
     JOIN   → validateJoinPurchaseOrder()
     BUY    → validateBuyPurchaseOrder()
     CHANGE → validateChangePurchaseOrder()
     COUPON → validateCouponPurchaseOrder()
```

**Por tipo:**

| Tipo | Validações executadas |
|------|-----------------------|
| `JOIN` | MGM (`memberGetMemberService.validate(mgm?.code)`) + cupom (se presente) + ofertas no catálogo + items **NÃO devem** ter productId |
| `BUY` | Cupom (se presente) + ofertas no catálogo + **TODOS** os offerItems devem ter productId |
| `CHANGE` | Ofertas no catálogo + **TODOS** os offerItems devem ter productId + `subscriptionId` obrigatório |
| `COUPON` | Se customer e coupon presentes: `couponService.validationPurchaseOrderCoupon()` + **TODOS** os offerItems devem ter productId |

**Serviços externos chamados durante validação:**

| Serviço | Quando | O que valida |
|---------|--------|-------------|
| `CustomerInfoService.validateCustomer(id)` | Sempre que `customer \!= null` | Cliente ativo no CIM |
| `CustomerInfoService.validateProduct(customerId, productId)` | JOIN/BUY/CHANGE, por offerItem com productId | Produto ativo para o cliente |
| `CatalogManagerService.validateOffers(items)` | JOIN/BUY/CHANGE se items não vazio | Ofertas existem no catálogo |
| `MemberGetMemberService.validate(code)` | JOIN | Código MGM válido |
| `CouponService.validateCoupon(coupon, customer)` | JOIN/BUY se coupon presente | Cupom válido para o cliente |
| `CouponService.validationPurchaseOrderCoupon(po)` | COUPON | Validação completa de cupom no contexto do pedido |

---

### 21.6 Command Handlers — O que cada um faz

#### `PurchaseOrderCommandHandler`
Injeta: `PurchaseOrderValidator`, `PurchaseOrderProducer`

| Método | Lógica |
|--------|--------|
| `handle(CreatePurchaseOrderCommand)` | `new PurchaseOrder(id, type, customer, callback)` → `save()` → `notify()` → retorna PurchaseOrder |
| `handle(UpdatePurchaseOrderType)` | `withPurchaseOrder(id)` → `verifyOpen()` + `verifyTypeNull()` → `applyChange(TypeUpdated)` |
| `handle(DeletePurchaseOrderCommand)` | `withPurchaseOrder(id)` → `canDelete()` → `applyChange(Deleted)` |
| `handle(ValidatePurchaseOrder)` | `getPurchaseOrder(id)` → `purchaseOrderValidator.validate(po)` — sem evento, sem save |
| `handle(FindPurchaseOrderCommand)` | `getPurchaseOrder(id)` — apenas leitura do Event Store |

#### `PurchaseOrderCustomerOrderCommandHandler`
Injeta: `PurchaseOrderValidator`, `PurchaseOrderCheckoutResolver`, `PurchaseOrderProducer`

| Método | Lógica |
|--------|--------|
| `handle(CheckoutCommand)` | `withPurchaseOrder(id)` → `command.execute(po, channel, checkoutFactory, validator, codes)` → `notify()` → retorna CustomerOrder |
| `handle(UpdateCustomerOrderCommand)` | `withPurchaseOrder(id)` → `po.updateCustomerOrder(customerOrder, producer, reason)` (pode emitir até 3 eventos e notificar Kafka) |

#### `PurchaseOrderItemCommandHandler`
| Método | Lógica |
|--------|--------|
| `handle(AddItemCommand)` | `withPurchaseOrder(id)` → `verifyOpen()` → `applyChange(ItemAdded)` |
| `handle(UpdateItemCommand)` | `withPurchaseOrder(id)` → `verifyOpen()` + `purchaseOrderHasItem(id)` → `applyChange(ItemUpdated)` |
| `handle(RemoveItemCommand)` | `withPurchaseOrder(id)` → `verifyOpen()` + `purchaseOrderHasItem(id)` → `applyChange(ItemRemoved)` |

#### `PurchaseOrderCouponCommandHandler`
| Método | Lógica |
|--------|--------|
| `handle(CreatePurchaseOrderCouponCommand)` | Cria PurchaseOrder + aplica cupom + save + notify |
| `handle(UpdateCouponCommand)` | `withPurchaseOrder(id)` → `validateType(BUY/JOIN)` + `hasCustomer()` + `couponService.validate()` → `applyChange(CouponUpdated)` |

#### `PurchaseOrderMgmCommandHandler`
| Método | Lógica |
|--------|--------|
| `handle(UpdateMgmCommand)` | `verifyOpen()` + `memberGetMemberService.validate(code)` → `applyChange(MgmUpdated)` |
| `handle(DeleteMgmCommand)` | `verifyOpen()` → `applyChange(MgmDeleted(null))` |

#### Handlers simples (padrão: `verifyOpen()` → `applyChange()`)
Todos seguem o mesmo padrão sem validações externas:

| Handler | Mutation | Evento |
|---------|----------|--------|
| `PurchaseOrderPaymentCommandHandler` | substitui `payment` | `PurchaseOrderPaymentUpdated` |
| `PurchaseOrderFreightCommandHandler` | substitui `freight` | `PurchaseOrderFreightUpdated` |
| `PurchaseOrderCustomerCommandHandler` | substitui `customer` | `PurchaseOrderCustomerUpdated` |
| `PurchaseOrderSegmentationCommandHandler` | substitui `segmentation` | `PurchaseOrderSegmentationUpdated` |
| `PurchaseOrderOnBoardingSaleCommandHandler` | substitui `onBoardingSale` | `PurchaseOrderOnBoardingSaleUpdated` |
| `PurchaseOrderSalesForceCommandHandler` | substitui/remove `salesForce` | `PurchaseOrderSalesForceUpdated` / `SalesForceRemoved` |
| `PurchaseOrderProtocolCommandHandler` | substitui `protocol` | `PurchaseOrderProtocolUpdated` |
| `PurchaseOrderSubscriptionCommandHandler` | substitui `subscriptionId` | `PurchaseOrderSubscriptionUpdated` |
| `PurchaseOrderInstallationAttributesCommandHandler` | upsert/remove em `installationAttributes` | `InstallationAttributesUpdated` / `Deleted` |

---

### 21.7 O Fluxo de Checkout — Detalhado

O checkout é a operação mais complexa. Envolve 3 camadas de validação e 2 estratégias de integração.

#### Sequência de execução em `CheckoutCommand.execute()`

```
CheckoutCommand.execute(purchaseOrder, channel, checkoutFactory, validator, securityCodes)
│
├── 1. verifyPurchaseOrderIsOpen()
│      └── status \!= OPENED → BusinessException(PURCHASE_ORDER_NOT_EXISTS)
│
├── 2. purchaseOrderValidator.validate(purchaseOrder)   ← PurchaseOrderValidationService
│      ├── validateCustomer() → CIM API
│      └── por tipo:
│          JOIN   → MGM + coupon + catálogo + sem productId
│          BUY    → coupon + catálogo + com productId
│          CHANGE → catálogo + com productId + subscriptionId
│          COUPON → cupom completo + com productId
│
├── 3. canCheckout()
│      ├── hasCustomer() → customer \!= null
│      ├── payment.methods.isEmpty() → BusinessException(PAYMENT_NOT_FOUND)
│      ├── items.isEmpty() → BusinessException(ITEMS_NOT_INFORMED)
│      └── métodos duplicados → PurchaseOrderValidationMethodsException(MORE_ONE_PAYMENT_METHOD)
│
├── 4. CheckoutFactory.resolve(purchaseOrder) → estratégia
│      ├── type == COUPON → PurchaseOrderCouponCheckoutService
│      └── qualquer outro → PurchaseOrderCheckoutService
│
├── 5. estratégia.checkout(purchaseOrder, channel, securityCodes)
│      │
│      ├── [PurchaseOrderCheckoutService — todos exceto COUPON]
│      │    ├── purchaseOrderValidator.validate() [segunda chamada\!]
│      │    ├── cmsOfferClient.offers(offerIds) → busca detalhes das ofertas
│      │    ├── CustomerOrderRequest.of(purchaseOrder, offerDetails, callbackUrl, ...)
│      │    └── customerOrderManagerApiService.checkoutPurchaseOrder(request)
│      │         └── retorna CustomerOrderResponse → .toCustomerOrder()
│      │
│      └── [PurchaseOrderCouponCheckoutService — type == COUPON]
│           ├── purchaseOrderValidator.validate() [segunda chamada\!]
│           ├── pcmClient.findOne(items[0].offerItems[0].catalogOfferItemId, "open")
│           ├── CustomerOrderRequest.of(purchaseOrder, compositionResponse, callbackUrl, ...)
│           └── customerOrderManagerApiService.checkoutPurchaseOrder(request)
│                └── retorna CustomerOrderResponse → .toCustomerOrder()
│
├── 6. purchaseOrder.customerOrder = resultado acima
│
└── 7. applyChange(PurchaseOrderCheckedOut(
         aggregateId = id,
         customerOrder = customerOrder,
         channel = channel,
         securityCodeInformed = securityCodes.map {
             SecurityCodeInformed(methodId, securityCodeInformed = \!code.isBlank())
         }
     ))
     └── evento aplica: status = CHECKED_OUT, channelCheckout = channel
```

> **Atenção — double validation:** O `purchaseOrderValidator.validate()` é chamado **duas vezes** no checkout: uma vez no `CheckoutCommand.execute()` e outra dentro da implementação de `PurchaseOrderCheckoutService.checkout()` / `PurchaseOrderCouponCheckoutService.checkout()`. Isso é um artefato do código legado.

#### Diferença entre as duas estratégias de checkout

| Aspecto | `PurchaseOrderCheckoutService` | `PurchaseOrderCouponCheckoutService` |
|---------|-------------------------------|--------------------------------------|
| Quando usar | `type \!= COUPON` | `type == COUPON` |
| Fonte dos detalhes | CMS (offers por id+tipo) | PCM (composition do primeiro offerItem) |
| Chamada | `cmsOfferClient.offers(offerIds)` | `pcmClient.findOne(catalogOfferItemId, "open")` |
| Limitação | Processa todos os itens | Só processa o **primeiro** item (hardcoded\!) |

---

### 21.8 Atualização via Callback do COM (`updateCustomerOrder`)

Quando o COM chama `POST /purchase-orders/callback`, este é o fluxo interno:

```kotlin
fun PurchaseOrder.updateCustomerOrder(customerOrder: CustomerOrder, producer, reason: Reason?) {
    // Sempre: atualiza o customerOrder
    applyChange(PurchaseOrderCustomerOrderUpdated(id, customerOrder))

    // Se há razão de erro: registra
    if (reason \!= null) applyChange(PurchaseOrderReasonStatusUpdated(id, reason))

    // Determina novo status baseado no status do COM:
    when (customerOrder.status.toUpperCase()) {
        "COMPLETED"              → applyChange(StatusUpdated(COMPLETED)) + producer.notify()
        "PARTIAL","REJECTED","FAILED" → applyChange(StatusUpdated(OPENED)) + producer.notify()
                                          ← volta para OPENED\! permite nova tentativa
        "CANCELED"               → applyChange(StatusUpdated(CANCELED)) + producer.notify()
    }
    // Se status for outro (ex: "PROCESSING"), nenhuma transição ocorre
}
```

**Mapeamento de status COM → PO:**

| Status do COM | Novo status do PO | Kafka notificado? |
|---------------|-------------------|-------------------|
| `"COMPLETED"` | `COMPLETED` | sim |
| `"PARTIAL"` | `OPENED` (rollback) | sim |
| `"REJECTED"` | `OPENED` (rollback) | sim |
| `"FAILED"` | `OPENED` (rollback) | sim |
| `"CANCELED"` | `CANCELED` | sim |
| qualquer outro (ex: `"PROCESSING"`) | sem mudança | não |

---

### 21.9 Cálculo de Desconto (totalPrice e discountPrice)

```kotlin
fun PurchaseOrder.totalPrice(): Price {
    return if (items.isNotEmpty()) {
        val finalPriceAmount = items.sumBy { it.firstPeriodPrice() } - discountPrice().amount
        Price(
            amount = if (finalPriceAmount > 0) finalPriceAmount else 0,  // nunca negativo
            scale  = items.first().price.scale,
            currency = items.first().price.currency
        )
    } else Price.zero()
}

fun PurchaseOrder.discountPrice(): Price =
    coupon?.reward?.type?.takeIf { items.isNotEmpty() }?.let { type ->
        when (type) {
            "DISCOUNT_MONEY"   → discountPriceMoney()   // valor fixo do cupom, capped pelo total
            "DISCOUNT_PERCENT" → discountPricePercent() // percentual do primeiro item
            else               → Price.zero()
        }
    } ?: Price.zero()
```

**Regras:**
- `totalPrice = soma(firstPeriodPrice de cada item) - desconto`
- `totalPrice` nunca é negativo (capped em 0)
- Desconto MONEY: `min(discount.amount, totalItems)` — não excede o total
- Desconto PERCENT: `(totalAmount * discountAsPercent).toInt()` — limitado ao total

---

### 21.10 Todos os Códigos de Erro

Fonte: `PurchaseOrderErrorCode`

| Código | Chave i18n | Cenário |
|--------|-----------|---------|
| `SLM-010` | purchase.order.checkout.error | Erro genérico no checkout |
| `PURCHASE_ORDER_NOT_EXISTS` | purchase.not.exists | Operação em pedido não-OPENED |
| `PURCHASE_ORDER_CANNOT_DELETE` | purchase.not.delete | Delete em pedido não-OPENED |
| `CATALOG_MANAGER_INTEGRATION_ERROR` | catalog.search.integration.error | Falha na integração com CMS |
| `EVENT_STORE_INTEGRATION_ERROR` | event.store.integration.error | Falha no Event Store |
| `CUSTOMER_INFO_INTEGRATION_ERROR` | customer.info.integration.error | Falha na integração com CIM |
| `CUSTOMER_ORDER_MANAGER_INTEGRATION_ERROR` | customer.order.manger.integration.error | Falha na integração com COM |
| `CUSTOMER_NOT_FOUND` | customer.not.found | Cliente não encontrado no CIM |
| `CUSTOMER_NOT_INFORMED` | customer.not.informed | Campo customer ausente no pedido |
| `CUSTOMER_INACTIVE` | customer.inactive | Cliente inativo no CIM |
| `PAYMENT_NOT_FOUND` | payment.not.found | Sem métodos de pagamento no checkout |
| `ALL_ITEMS_MUST_HAVE_PRODUCTID` | purchase.items.must.have.productId.error | Items sem productId em BUY/CHANGE |
| `PRICE_MUST_BE_INFORMED` | price.must.be.informed | Preço obrigatório ausente |
| `INCOMPATIBLE_PURCHASE_ORDER` | incompatible.purchase.order | Pedido incompatível com a operação |
| `PURCHASE_ORDER_INVALID_TYPE` | purchase.order.invalid.type | Type inválido para cupom (não é BUY/JOIN) |
| `PURCHASE_ORDER_TYPE_IS_ALREADY_DEFINED` | purchase.order.type.is.already.defined | Tentativa de redefinir type já definido |
| `PURCHASE_ORDER_CALLBACK_INTEGRATION_ERROR` | purchase.order.callback.integration.error | Falha ao notificar callback |
| `CATALOG_OFFER_NOT_RETURNED_BY_OFFER_DETAILS` | catalog.offer.not.returned.by.offer.details | Oferta não encontrada no catálogo |
| `CATALOG_OFFER_HAS_NO_DESCRIPTION` | catalog.offer.has.no.description | Oferta sem descrição no catálogo |
| `CATALOG_OFFER_NOT_RETURNED_BY_OFFER_ITEM_DETAILS` | catalog.offer.not.returned.by.offer.item.details | Item de oferta não encontrado |
| `PRODUCT_NOT_FOUND` | product.not.found | Produto não encontrado |
| `PRODUCT_INACTIVE` | product.inactive | Produto inativo |
| `COUPON_INTEGRATION_ERROR` | coupon.integration.error | Falha na integração com Coupon Service |
| `COUPON_VALIDATION_ERROR` | coupon.validation.type.error | Tipo de cupom inválido |
| `COUPON_NOT_FOUND` | coupon.not.found | Cupom não encontrado |
| `COUPON_DISCOUNT_NOT_INFORMED` | coupon.discount.not.informed | Desconto do cupom ausente |
| `COUPON_INACTIVE` | coupon.inactive | Cupom inativo |
| `COUPON_SEGMENT_NOT_SUPPORTED` | coupon.segment.not.supported | Segmento do cupom não suportado |
| `COMPOSITION_ID_INVALID` | composition.id.invalid | ID de composição inválido |
| `CATALOG_SEARCH_OFFER_INVALID` | catalog.search.offer.invalid | Oferta inválida no catálogo |
| `CATALOG_SEARCH_VALUE_ATTRIBUTE_ERROR` | catalog.search.value.attribute.error | Atributo de oferta com valor inválido |
| `ITEMS_NOT_INFORMED` | items.not.informed | Sem itens no pedido durante checkout |
| `SUBSCRIPTION_ID_NOT_INFORMED` | subscription.id.not.informed | subscriptionId ausente em CHANGE |
| `NO_NEED_PRODUCT_ID_TO_TYPE_JOIN` | no.need.product.id.to.type.join | Items com productId em pedido JOIN |
| `MORE_ONE_PAYMENT_METHOD` | purchase.methods.more.one | Métodos de pagamento duplicados |
