# Especificação Técnica: Sales Manager Rebuild

> **Referência:** `docs/migration-plan.md` — estratégia e decisões arquiteturais  
> **Fonte legada:** `legacy_sales_manager/analise-completa.md` + contratos de API  
> **Estado atual:** Fase 0 não iniciada — nenhum código produzido  

---

## 1. Estrutura Maven Multi-Module

### 1.1 Hierarquia de Módulos

```
sales-manager/                     # Parent POM
├── domain/                        # Kotlin puro, ZERO Spring
├── api/                           # Contratos REST (Spring MVC annotations)
├── infrastructure/                # Spring + recursos externos
├── query/                         # Projeção CQRS (read side)
├── command-app/                   # App Spring Boot write (porta 8080)
├── query-app/                     # App Spring Boot read (porta 8180)
└── consumer-app/                  # App Kafka consumer (porta 8082)
```

### 1.2 Parent POM (`pom.xml`)

```xml
<groupId>br.com.zup.realwave</groupId>
<artifactId>sales-manager</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.4.x</version>
</parent>

<properties>
  <java.version>21</java.version>
  <kotlin.version>2.1.x</kotlin.version>
  <spring-cloud.version>2024.0.x</spring-cloud.version>
  <testcontainers.version>1.20.x</testcontainers.version>
</properties>
```

**Dependências gerenciadas no Parent POM:**

| Artefato | Versão | Uso |
|---|---|---|
| `spring-boot-starter-web` | via BOM | Command/Query controllers |
| `spring-boot-starter-jdbc` | via BOM | JdbcEventStore, query repos |
| `spring-boot-starter-oauth2-resource-server` | via BOM | Auth (substitui Keycloak adapter) |
| `spring-cloud-starter-openfeign` | via BOM | 6 Feign clients |
| `spring-kafka` | via BOM | Producer + Consumer |
| `liquibase-core` | 4.29.x | Migrations per-tenant |
| `jackson-module-kotlin` | via BOM | Serialização eventos |
| `postgresql` | 42.7.x | Driver JDBC |
| `mockk` | 1.13.x | Testes unitários |
| `testcontainers` | via BOM | Testes de integração |

### 1.3 Dependências por Módulo

```
domain          → kotlin-stdlib, jackson-annotations
api             → domain, spring-web
infrastructure  → domain, api, spring-boot-starter-web, spring-boot-starter-jdbc,
                  spring-boot-starter-oauth2-resource-server, spring-cloud-starter-openfeign,
                  spring-kafka, liquibase-core, postgresql, jackson-module-kotlin
query           → domain, api, infrastructure
command-app     → domain, api, infrastructure
query-app       → query, api, infrastructure
consumer-app    → infrastructure
```

---

## 2. Módulo `domain/`

> **Regra absoluta:** Zero dependências Spring. Apenas `kotlin-stdlib` e `jackson-annotations`.

### 2.1 Classe Base `AggregateRoot`

```kotlin
// domain/src/main/kotlin/br/com/zup/realwave/sales/manager/domain/AggregateRoot.kt
abstract class AggregateRoot<ID> {
    abstract val id: ID
    private val _pendingEvents: MutableList<DomainEvent> = mutableListOf()
    val pendingEvents: List<DomainEvent> get() = _pendingEvents.toList()
    var version: Long = 0L
        protected set

    protected fun applyChange(event: DomainEvent) {
        replayEvent(event)
        _pendingEvents.add(event)
    }

    fun replayEvent(event: DomainEvent) {
        event.apply(this)
        version++
    }

    fun clearPendingEvents() = _pendingEvents.clear()
}
```

### 2.2 Interface `DomainEvent`

```kotlin
// domain/src/main/kotlin/br/com/zup/realwave/sales/manager/domain/DomainEvent.kt
interface DomainEvent {
    fun apply(aggregate: AggregateRoot<*>)
}
```

### 2.3 Aggregate Root: `PurchaseOrder`

**Arquivo:** `domain/src/main/kotlin/br/com/zup/realwave/sales/manager/domain/PurchaseOrder.kt`

**Estado (campos do aggregate):**

| Campo | Tipo | Nullable | Valor inicial |
|---|---|---|---|
| `id` | `PurchaseOrderId` | não | obrigatório |
| `status` | `PurchaseOrderStatus` | não | `OPENED` |
| `type` | `PurchaseOrderType` | sim | `null` |
| `customer` | `Customer` | sim | `null` |
| `callback` | `Callback` | sim | `null` |
| `items` | `MutableSet<Item>` | não | `mutableSetOf()` |
| `payment` | `Payment` | não | `Payment()` |
| `freight` | `Freight` | sim | `null` |
| `coupon` | `CouponCode` | sim | `null` |
| `customerOrder` | `CustomerOrder` | sim | `null` |
| `protocol` | `Protocol` | sim | `null` |
| `subscriptionId` | `SubscriptionId` | sim | `null` |
| `channelCreate` | `Channel` | sim | `null` |
| `channelCheckout` | `Channel` | sim | `null` |
| `segmentation` | `Segmentation` | sim | `null` |
| `onBoardingSale` | `OnBoardingSale` | sim | `null` |
| `mgm` | `Mgm` | sim | `null` |
| `salesForce` | `SalesForce` | sim | `null` |
| `installationAttributes` | `MutableMap<ProductTypeId, InstallationAttribute>` | não | `mutableMapOf()` |
| `reason` | `Reason` | sim | `null` |
| `securityCodeInformed` | `List<SecurityCodeInformed>` | não | `emptyList()` |
| `createdAt` | `String` | sim | `null` |
| `updatedAt` | `String` | sim | `null` |

**Transições de status permitidas:**

```
OPENED → CHECKED_OUT  (via CheckoutCommand)
OPENED → DELETED      (via DeletePurchaseOrderCommand)
CHECKED_OUT → COMPLETED  (via CustomerOrderCallback com status COMPLETED)
CHECKED_OUT → FAILED     (via CustomerOrderCallback com status FAILED)
CHECKED_OUT → CANCELED   (via CustomerOrderCallback com status CANCELED)
```

**Factory method:**
```kotlin
companion object {
    fun create(
        id: PurchaseOrderId,
        type: PurchaseOrderType?,
        customer: Customer?,
        callback: Callback?,
        channel: Channel?
    ): PurchaseOrder
}
```

### 2.4 Value Objects

#### `PurchaseOrderId`
```kotlin
data class PurchaseOrderId(val value: String = UUID.randomUUID().toString())
```

#### `PurchaseOrderStatus` (enum)
```kotlin
enum class PurchaseOrderStatus { OPENED, CHECKED_OUT, COMPLETED, FAILED, CANCELED, DELETED }
```

#### `PurchaseOrderType` (enum)
```kotlin
enum class PurchaseOrderType { NORMAL, ONBOARDING }
```

#### `Customer`
```kotlin
data class Customer(val id: String)
```

#### `Channel`
```kotlin
data class Channel(val id: String, val type: String)
```

#### `Callback`
```kotlin
data class Callback(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)
```

#### `Item`
```kotlin
data class Item(
    val id: ItemId,
    val catalogOfferId: String,
    val price: Price,
    val validity: OfferValidity?,
    val offerItems: List<OfferItem>,
    val pricesPerPeriod: List<PricePerPeriod>?
)
```

#### `Price`
```kotlin
data class Price(
    val currency: String,
    val amount: Long,    // centavos
    val scale: Int = 2
)
```

#### `OfferValidity`
```kotlin
data class OfferValidity(
    val period: String,
    val duration: Int,
    val unlimited: Boolean
)
```

#### `OfferItem`
```kotlin
data class OfferItem(
    val id: String,
    val productId: String,
    val price: Price,
    val quantity: Int?
)
```

#### `PricePerPeriod`
```kotlin
data class PricePerPeriod(
    val period: String,
    val price: Price
)
```

#### `Payment`
```kotlin
data class Payment(
    val methods: MutableList<PaymentMethod> = mutableListOf(),
    val description: String? = null
) {
    fun couponPayment(): Payment  // retorna Payment com desconto aplicado
}
```

#### `PaymentMethod`
```kotlin
data class PaymentMethod(
    val type: String,
    val installments: Int?,
    val installmentValue: Price?,
    val totalValue: Price?,
    val cardToken: String?
)
```

#### `Freight`
```kotlin
data class Freight(
    val price: Price,
    val deliveryEstimateBusinessDays: Int?,
    val latitude: String?,
    val longitude: String?,
    val type: String?,
    val address: Address?
)
```

#### `Address`
```kotlin
data class Address(
    val street: String,
    val number: String?,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val country: String,
    val zipCode: String,
    val referencePoint: String?
)
```

#### `CouponCode`
```kotlin
data class CouponCode(val code: String)
```

#### `CustomerOrder`
```kotlin
data class CustomerOrder(val id: String)
```

#### `Protocol`
```kotlin
data class Protocol(val value: String)
```

#### `SubscriptionId`
```kotlin
data class SubscriptionId(val value: String)
```

#### `Segmentation`
```kotlin
data class Segmentation(val customFields: Map<String, Any>)
```

#### `OnBoardingSale`
```kotlin
data class OnBoardingSale(val customFields: Map<String, Any>)
```

#### `Mgm`
```kotlin
data class Mgm(val code: String, val customFields: Map<String, Any>?)
```

#### `SalesForce`
```kotlin
data class SalesForce(
    val agentId: String?,
    val supervisorId: String?,
    val channel: String?
)
```

#### `InstallationAttribute`
```kotlin
data class InstallationAttribute(
    val productTypeId: ProductTypeId,
    val attributes: Map<String, Any>
)
```

#### `ProductTypeId`
```kotlin
data class ProductTypeId(val value: String)
```

#### `Reason`
```kotlin
data class Reason(val code: String, val message: String?)
```

#### `SecurityCodeInformed`
```kotlin
data class SecurityCodeInformed(val catalogOfferItemId: String, val code: String)
```

#### `Discount`
```kotlin
data class Discount(
    val percentage: BigDecimal,
    val valueDiscount: Price,
    val coupon: String?
)
```

### 2.5 Eventos de Domínio

**Localização:** `domain/src/main/kotlin/br/com/zup/realwave/sales/manager/domain/event/`

Todos implementam `DomainEvent`. A classe base abstrata `PurchaseOrderApplicableEvent` implementa `fun apply(aggregate: AggregateRoot<*>)` com cast para `PurchaseOrder`.

| # | Evento | Campos | Efeito em `apply()` |
|---|---|---|---|
| 1 | `PurchaseOrderCreated` | id, type, customer, callback, channel | status=OPENED, channelCreate |
| 2 | `PurchaseOrderTypeUpdated` | id, type | type |
| 3 | `PurchaseOrderDeleted` | id | status=DELETED |
| 4 | `PurchaseOrderItemAdded` | id, item | items.add(item) |
| 5 | `PurchaseOrderItemRemoved` | id, catalogOfferId | items.remove onde catalogOfferId |
| 6 | `PurchaseOrderItemUpdated` | id, item | items: substituir por item |
| 7 | `PurchaseOrderPaymentUpdated` | id, payment | payment |
| 8 | `PurchaseOrderFreightUpdated` | id, freight | freight |
| 9 | `PurchaseOrderCouponUpdated` | id, coupon | coupon |
| 10 | `PurchaseOrderCustomerUpdated` | id, customer | customer |
| 11 | `PurchaseOrderProtocolUpdated` | id, protocol | protocol |
| 12 | `PurchaseOrderSubscriptionUpdated` | id, subscriptionId | subscriptionId |
| 13 | `PurchaseOrderSegmentationUpdated` | id, segmentation | segmentation |
| 14 | `PurchaseOrderOnBoardingSaleUpdated` | id, onBoardingSale | onBoardingSale |
| 15 | `PurchaseOrderMgmUpdated` | id, mgm | mgm |
| 16 | `PurchaseOrderMgmDeleted` | id | mgm=null |
| 17 | `PurchaseOrderSalesForceUpdated` | id, salesForce | salesForce |
| 18 | `PurchaseOrderSalesForceRemoved` | id | salesForce=null |
| 19 | `PurchaseOrderInstallationAttributesUpdated` | id, productTypeId, attributes | installationAttributes[productTypeId] |
| 20 | `PurchaseOrderInstallationAttributesDeleted` | id, productTypeId | installationAttributes.remove(productTypeId) |
| 21 | `PurchaseOrderCustomerOrderUpdated` | id, customerOrder, status, channel | customerOrder, status, channelCheckout |
| 22 | `PurchaseOrderStatusUpdated` | id, status, reason | status, reason |
| 23 | `PurchaseOrderReasonStatusUpdated` | id, reason, status | reason, status |
| 24 | `PurchaseOrderCheckedOut` | id, customerOrder, channel, securityCodes | status=CHECKED_OUT, customerOrder, channelCheckout, securityCodeInformed |
| 25 | `PurchaseOrderCouponCreated` | id, type, customer, callback, channel, coupon | status=OPENED, coupon |

### 2.6 Commands

**Localização:** `domain/src/main/kotlin/br/com/zup/realwave/sales/manager/domain/command/`

| Command | Campos |
|---|---|
| `CreatePurchaseOrderCommand` | id, type, customer, callback, channel |
| `CreatePurchaseOrderCouponCommand` | id, type, customer, callback, channel, coupon, items, payment |
| `DeletePurchaseOrderCommand` | id |
| `UpdatePurchaseOrderTypeCommand` | id, type |
| `ValidatePurchaseOrderCommand` | id |
| `FindPurchaseOrderCommand` | id |
| `AddItemCommand` | id, item |
| `UpdateItemCommand` | id, itemId, item |
| `RemoveItemCommand` | id, catalogOfferId |
| `UpdatePaymentCommand` | id, payment |
| `UpdateFreightCommand` | id, freight |
| `UpdateCouponCommand` | id, coupon |
| `UpdateCustomerCommand` | id, customer |
| `UpdateProtocolCommand` | id, protocol |
| `UpdateSubscriptionCommand` | id, subscriptionId |
| `UpdateSegmentationCommand` | id, segmentation |
| `UpdateOnBoardingSaleCommand` | id, onBoardingSale |
| `UpdateMgmCommand` | id, mgm |
| `DeleteMgmCommand` | id |
| `UpdateSalesForceCommand` | id, salesForce |
| `RemoveSalesForceCommand` | id |
| `UpdateInstallationAttributesCommand` | id, productTypeId, attributes |
| `DeleteInstallationAttributesCommand` | id, productTypeId |
| `UpdateCustomerOrderCommand` | id, customerOrder, status, reason, channel |
| `CheckoutCommand` | id, paymentSecurityCodes, channel |

### 2.7 Ports (Interfaces)

```kotlin
// Repositório de escrita
interface PurchaseOrderRepository {
    fun save(purchaseOrder: PurchaseOrder)
    fun findById(id: PurchaseOrderId): PurchaseOrder?
    fun findByIdOrThrow(id: PurchaseOrderId): PurchaseOrder
}

// Serviços de domínio
interface PurchaseOrderValidator {
    fun validate(purchaseOrder: PurchaseOrder)
}

interface PurchaseOrderCheckoutService {
    fun checkout(purchaseOrder: PurchaseOrder, command: CheckoutCommand): CustomerOrder
}

interface PurchaseOrderEventPublisher {
    fun publish(purchaseOrder: PurchaseOrder)
}
```

---

## 3. Módulo `api/`

> Contém interfaces REST anotadas com Spring MVC + DTOs. Implementado por `command-app` e `query-app`.

### 3.1 Interfaces REST

#### `PurchaseOrderCommandApi`

```kotlin
@RequestMapping("/purchase-orders")
interface PurchaseOrderCommandApi {
    @PostMapping
    fun create(@RequestBody request: PurchaseOrderRequest, ...): ResponseEntity<CreatePurchaseOrderResponse>

    @PostMapping("/coupon")
    fun createWithCoupon(@RequestBody request: PurchaseOrderCouponRequest, ...): ResponseEntity<CreatePurchaseOrderResponse>

    @DeleteMapping("/{purchaseOrderId}")
    fun delete(@PathVariable purchaseOrderId: String, ...): ResponseEntity<DeleteResponse>

    @PutMapping("/{purchaseOrderId}/type")
    fun updateType(@PathVariable purchaseOrderId: String, @RequestBody request: UpdateTypeRequest, ...): ResponseEntity<Void>

    @GetMapping("/{purchaseOrderId}/validation")
    fun validate(@PathVariable purchaseOrderId: String, ...): ResponseEntity<Void>

    @PostMapping("/{purchaseOrderId}/items")
    fun addItem(@PathVariable purchaseOrderId: String, @RequestBody request: ItemRequest, ...): ResponseEntity<Void>

    @PutMapping("/{purchaseOrderId}/items/{itemId}")
    fun updateItem(@PathVariable purchaseOrderId: String, @PathVariable itemId: String, @RequestBody request: ItemRequest, ...): ResponseEntity<Void>

    @DeleteMapping("/{purchaseOrderId}/items/{catalogOfferId}")
    fun removeItem(@PathVariable purchaseOrderId: String, @PathVariable catalogOfferId: String, ...): ResponseEntity<Void>

    @PutMapping("/{purchaseOrderId}/payment")
    fun updatePayment(@PathVariable purchaseOrderId: String, @RequestBody request: PaymentRequest, ...): ResponseEntity<UpdatePaymentResponse>

    @PutMapping("/{purchaseOrderId}/freight")
    fun updateFreight(@PathVariable purchaseOrderId: String, @RequestBody request: FreightRequest, ...): ResponseEntity<UpdateFreightResponse>

    @PutMapping("/{purchaseOrderId}/coupon")
    fun updateCoupon(@PathVariable purchaseOrderId: String, @RequestBody request: CouponRequest, ...): ResponseEntity<UpdateCouponResponse>

    @PutMapping("/{purchaseOrderId}/customer")
    fun updateCustomer(@PathVariable purchaseOrderId: String, @RequestBody request: CustomerRequest, ...): ResponseEntity<UpdateCustomerIdResponse>

    @PutMapping("/{purchaseOrderId}/protocol")
    fun updateProtocol(@PathVariable purchaseOrderId: String, @RequestBody request: ProtocolRequest, ...): ResponseEntity<ProtocolResponse>

    @PutMapping("/{purchaseOrderId}/subscription")
    fun updateSubscription(@PathVariable purchaseOrderId: String, @RequestBody request: SubscriptionRequest, ...): ResponseEntity<SubscriptionResponse>

    @PutMapping("/{purchaseOrderId}/segmentation")
    fun updateSegmentation(@PathVariable purchaseOrderId: String, @RequestBody request: SegmentationRequest, ...): ResponseEntity<SegmentationResponse>

    @PutMapping("/{purchaseOrderId}/onboarding-sale")
    fun updateOnBoardingSale(@PathVariable purchaseOrderId: String, @RequestBody request: OnBoardingSaleRequest, ...): ResponseEntity<UpdateOnBoardingSaleResponse>

    @PutMapping("/{purchaseOrderId}/mgm")
    fun updateMgm(@PathVariable purchaseOrderId: String, @RequestBody request: MgmRequest, ...): ResponseEntity<PurchaseOrderMgmResponse>

    @DeleteMapping("/{purchaseOrderId}/mgm")
    fun deleteMgm(@PathVariable purchaseOrderId: String, ...): ResponseEntity<Void>

    @PutMapping("/{purchaseOrderId}/sales-force")
    fun updateSalesForce(@PathVariable purchaseOrderId: String, @RequestBody request: SalesForceRequest, ...): ResponseEntity<PurchaseOrderSalesForceResponse>

    @DeleteMapping("/{purchaseOrderId}/sales-force")
    fun removeSalesForce(@PathVariable purchaseOrderId: String, ...): ResponseEntity<Void>

    @PutMapping("/{purchaseOrderId}/installation-attributes")
    fun updateInstallationAttributes(@PathVariable purchaseOrderId: String, @RequestBody request: InstallationAttributesRequest, ...): ResponseEntity<UpdateInstallationAttributesResponse>

    @DeleteMapping("/{purchaseOrderId}/installation-attributes/{productTypeId}")
    fun deleteInstallationAttributes(@PathVariable purchaseOrderId: String, @PathVariable productTypeId: String, ...): ResponseEntity<DeleteInstallationAttributesResponse>

    @PostMapping("/{purchaseOrderId}/checkout")
    fun checkout(@PathVariable purchaseOrderId: String, @RequestBody request: CheckoutRequest, ...): ResponseEntity<CheckoutResponse>

    @PostMapping("/callback")
    fun customerOrderCallback(@RequestBody request: CustomerOrderCallbackRequest, ...): ResponseEntity<Void>
}
```

#### `PurchaseOrderQueryApi`

```kotlin
@RequestMapping("/purchase-orders")
interface PurchaseOrderQueryApi {
    @GetMapping("/{purchaseOrderId}")
    fun findById(@PathVariable purchaseOrderId: String, ...): ResponseEntity<PurchaseOrderResponse>

    @GetMapping("/{protocol}/protocol")
    fun findByProtocol(@PathVariable protocol: String, ...): ResponseEntity<PurchaseOrderResponse>

    @GetMapping("/{purchaseOrderId}/status")
    fun getStatus(@PathVariable purchaseOrderId: String, ...): ResponseEntity<PurchaseOrderStatusResponse>

    @GetMapping
    fun findByCustomer(
        @RequestParam customerId: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?,
        ...
    ): ResponseEntity<List<PurchaseOrderResponse>>
}
```

### 3.2 Request DTOs

#### `PurchaseOrderRequest`
```json
{
  "type": "NORMAL",
  "customer": "customer-uuid",
  "callback": {
    "url": "https://example.com/callback",
    "headers": { "X-Key": "value" }
  }
}
```

#### `PurchaseOrderCouponRequest`
```json
{
  "type": "NORMAL",
  "customer": "customer-uuid",
  "callback": { ... },
  "coupon": "COUPON123",
  "items": [...],
  "payment": { ... }
}
```

#### `ItemRequest`
```json
{
  "catalogOfferId": "offer-uuid",
  "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
  "validity": { "period": "MONTHLY", "duration": 12, "unlimited": false },
  "offerItems": [
    { "id": "item-uuid", "productId": "prod-uuid", "price": {...}, "quantity": 1 }
  ],
  "pricesPerPeriod": [
    { "period": "MONTHLY", "price": {...} }
  ]
}
```

#### `PaymentRequest`
```json
{
  "methods": [
    {
      "type": "CREDIT_CARD",
      "installments": 1,
      "installmentValue": { "currency": "BRL", "amount": 9990 },
      "totalValue": { "currency": "BRL", "amount": 9990 },
      "cardToken": "token-uuid"
    }
  ],
  "description": "Pagamento mensal"
}
```

#### `FreightRequest`
```json
{
  "price": { "currency": "BRL", "amount": 1500 },
  "deliveryEstimateBusinessDays": 5,
  "type": "SEDEX",
  "address": {
    "street": "Rua X", "number": "100", "complement": "Apto 1",
    "neighborhood": "Centro", "city": "SP", "state": "SP",
    "country": "BRA", "zipCode": "01310-100", "referencePoint": null
  }
}
```

#### `CheckoutRequest`
```json
{
  "paymentSecurityCodes": [
    { "catalogOfferItemId": "item-uuid", "code": "123" }
  ]
}
```

#### `CustomerOrderCallbackRequest`
```json
{
  "purchaseOrderId": "po-uuid",
  "customerOrderId": "co-uuid",
  "status": "COMPLETED",
  "reason": { "code": "SUCCESS", "message": null },
  "channel": { "id": "ch-id", "type": "WEB" }
}
```

### 3.3 Response DTOs

#### `CreatePurchaseOrderResponse`
```json
{ "id": "po-uuid" }
```

#### `CheckoutResponse`
```json
{
  "id": "po-uuid",
  "customerOrder": { "id": "co-uuid" }
}
```

#### `PurchaseOrderResponse` (response completo)
```json
{
  "id": "po-uuid",
  "status": "OPENED",
  "type": "NORMAL",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "customer": { "id": "cust-uuid" },
  "callback": { "url": "...", "headers": {} },
  "reason": null,
  "segmentation": { "customFields": {} },
  "mgm": { "code": "MGM123", "customFields": {} },
  "salesForce": { "agentId": "...", "supervisorId": "...", "channel": "..." },
  "onBoardingSale": { "customFields": {} },
  "coupon": "COUPON123",
  "totalPrice": { "currency": "BRL", "amount": 9990 },
  "discount": { "percentage": 10.0, "valueDiscount": {...}, "coupon": "COUPON123" },
  "payment": {
    "methods": [...],
    "description": "..."
  },
  "freight": { ... },
  "items": [
    {
      "id": "item-uuid",
      "catalogOfferId": "offer-uuid",
      "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
      "validity": { ... },
      "offerItems": [ ... ],
      "pricesPerPeriod": [ ... ]
    }
  ],
  "installationAttributes": {
    "PRODUCT_TYPE_1": { "attr1": "value1" }
  },
  "customerOrder": { "id": "co-uuid" },
  "protocol": "PROTOCOL-001",
  "subscriptionId": "sub-uuid",
  "channelCreate": { "id": "...", "type": "WEB" },
  "channelCheckout": { "id": "...", "type": "WEB" }
}
```

#### `PurchaseOrderStatusResponse`
```json
{ "id": "po-uuid", "status": "CHECKED_OUT" }
```

#### `DeleteResponse`
```json
{ "id": "po-uuid" }
```

### 3.4 Validadores

```kotlin
// api/src/main/kotlin/.../validator/ItemRequestValidator.kt
class ItemRequestValidator : Validator {
    override fun supports(clazz: Class<*>): Boolean
    override fun validate(target: Any, errors: Errors)
    // Valida: catalogOfferId obrigatório, price obrigatório, offerItems não vazio
}

// api/src/main/kotlin/.../validator/PaymentMethodValidation.kt
class PaymentMethodValidation : Validator {
    // Valida: type obrigatório, métodos válidos (CREDIT_CARD, DEBIT_CARD, BOLETO)
}
```

---

## 4. Módulo `infrastructure/`

### 4.1 Event Sourcing: `JdbcEventStore`

**Schema SQL (criado via Liquibase, per-tenant):**
```sql
-- Tabela domain_events (dentro de cada schema rw_sm_{tenant})
CREATE TABLE domain_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(36) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    metadata        JSONB,
    version         BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (aggregate_id, version)
);
CREATE INDEX idx_domain_events_aggregate ON domain_events(aggregate_id, version);
```

**Implementação:**
```kotlin
@Repository
class JdbcEventStore(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) : PurchaseOrderRepository {

    override fun save(purchaseOrder: PurchaseOrder) {
        // 1. Para cada pendingEvent: INSERT INTO domain_events
        // 2. purchaseOrder.clearPendingEvents()
        // 3. Inserir em outbox na mesma transação
    }

    override fun findById(id: PurchaseOrderId): PurchaseOrder? {
        // 1. SELECT * FROM domain_events WHERE aggregate_id = ? ORDER BY version
        // 2. Criar PurchaseOrder vazio
        // 3. Para cada evento: purchaseOrder.replayEvent(event)
        // 4. Retornar null se sem eventos
    }
}
```

**Event Type Registry:**
```kotlin
object EventTypeRegistry {
    private val types: Map<String, KClass<out DomainEvent>> = mapOf(
        "PurchaseOrderCreated"                          to PurchaseOrderCreated::class,
        "PurchaseOrderTypeUpdated"                      to PurchaseOrderTypeUpdated::class,
        "PurchaseOrderDeleted"                          to PurchaseOrderDeleted::class,
        "PurchaseOrderItemAdded"                        to PurchaseOrderItemAdded::class,
        "PurchaseOrderItemRemoved"                      to PurchaseOrderItemRemoved::class,
        "PurchaseOrderItemUpdated"                      to PurchaseOrderItemUpdated::class,
        "PurchaseOrderPaymentUpdated"                   to PurchaseOrderPaymentUpdated::class,
        "PurchaseOrderFreightUpdated"                   to PurchaseOrderFreightUpdated::class,
        "PurchaseOrderCouponUpdated"                    to PurchaseOrderCouponUpdated::class,
        "PurchaseOrderCustomerUpdated"                  to PurchaseOrderCustomerUpdated::class,
        "PurchaseOrderProtocolUpdated"                  to PurchaseOrderProtocolUpdated::class,
        "PurchaseOrderSubscriptionUpdated"              to PurchaseOrderSubscriptionUpdated::class,
        "PurchaseOrderSegmentationUpdated"              to PurchaseOrderSegmentationUpdated::class,
        "PurchaseOrderOnBoardingSaleUpdated"            to PurchaseOrderOnBoardingSaleUpdated::class,
        "PurchaseOrderMgmUpdated"                       to PurchaseOrderMgmUpdated::class,
        "PurchaseOrderMgmDeleted"                       to PurchaseOrderMgmDeleted::class,
        "PurchaseOrderSalesForceUpdated"                to PurchaseOrderSalesForceUpdated::class,
        "PurchaseOrderSalesForceRemoved"                to PurchaseOrderSalesForceRemoved::class,
        "PurchaseOrderInstallationAttributesUpdated"    to PurchaseOrderInstallationAttributesUpdated::class,
        "PurchaseOrderInstallationAttributesDeleted"    to PurchaseOrderInstallationAttributesDeleted::class,
        "PurchaseOrderCustomerOrderUpdated"             to PurchaseOrderCustomerOrderUpdated::class,
        "PurchaseOrderStatusUpdated"                    to PurchaseOrderStatusUpdated::class,
        "PurchaseOrderReasonStatusUpdated"              to PurchaseOrderReasonStatusUpdated::class,
        "PurchaseOrderCheckedOut"                       to PurchaseOrderCheckedOut::class,
        "PurchaseOrderCouponCreated"                    to PurchaseOrderCouponCreated::class,
    )

    fun resolve(type: String): KClass<out DomainEvent> =
        types[type] ?: error("Unknown event type: $type")
}
```

### 4.2 Transactional Outbox

```sql
-- Tabela outbox (mesma transação que domain_events)
CREATE TABLE outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(36) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Fluxo:**
1. Command handler salva events em `domain_events` + insere em `outbox` — **mesma transação JDBC**
2. `OutboxPoller` (`@Scheduled`) lê registros `published=false`, publica no Kafka, marca `published=true`

### 4.3 Multi-Tenancy

**Header de identificação:** `X-Realwave-Organization-Slug`

#### `TenantContext`
```kotlin
object TenantContext {
    private val tenant = ThreadLocal<String>()
    fun set(tenantId: String) { tenant.set(tenantId) }
    fun get(): String = tenant.get() ?: error("No tenant in context")
    fun clear() { tenant.remove() }
}
```

#### `TenantFilter`
```kotlin
@Component
class TenantFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val tenant = request.getHeader("X-Realwave-Organization-Slug")
            ?: return response.sendError(400, "Missing X-Realwave-Organization-Slug")
        try {
            TenantContext.set(tenant)
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
```

#### `TenantRoutingDataSource`
```kotlin
class TenantRoutingDataSource(
    private val dataSourceMap: Map<String, DataSource>,
    private val defaultDataSource: DataSource
) : AbstractRoutingDataSource() {
    override fun determineCurrentLookupKey(): String = TenantContext.get()
    // Cria DataSource on-demand para novos tenants
}
```

#### `LiquibaseHandler`
```kotlin
@Component
class LiquibaseHandler(private val dataSource: DataSource) {
    fun initializeTenantSchema(tenantId: String) {
        // 1. CREATE SCHEMA IF NOT EXISTS rw_sm_{tenantId}
        // 2. Executar migrations Liquibase no schema do tenant
        //    (domain_events, outbox, tabelas de query)
    }
}
```

### 4.4 Segurança (OAuth2 Resource Server)

```yaml
# application.yml (command-app/query-app/consumer-app)
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:https://keycloak.example.com/realms/zup}
```

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/**").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
        return http.build()
    }
}
```

### 4.5 Feign Clients

**Config base:**
```kotlin
@Configuration
class FeignConfig {
    @Bean
    fun requestInterceptor() = RequestInterceptor { template ->
        template.header("X-Realwave-Organization-Slug", TenantContext.get())
        // outros headers de contexto
    }
}
```

**6 Clientes:**

| Interface | URL Config | Uso |
|---|---|---|
| `CmsClient` | `cms.url` | Validar ofertas do catálogo |
| `PcmClient` | `pcm.url` | Validar produtos |
| `CimClient` | `cim.url` | Validar cliente |
| `CouponClient` | `coupon.url` | Validar e aplicar cupons |
| `MgmClient` | `mgm.url` | Validar código MGM |
| `ComClient` | `com.url` | Enviar pedido no checkout |

**Exemplo:**
```kotlin
@FeignClient(name = "com-client", url = "\${com.url}", configuration = [FeignConfig::class])
interface ComClient {
    @PostMapping("/customer-orders")
    fun createCustomerOrder(@RequestBody request: CustomerOrderRequest): CustomerOrderResponse
}
```

### 4.6 Error Handling

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    // Formato de resposta compatível com legado:
    // { "errors": [{ "code": "ERROR_CODE", "message": "description" }] }

    @ExceptionHandler(PurchaseOrderNotFoundException::class)
    fun handleNotFound(ex: PurchaseOrderNotFoundException): ResponseEntity<ErrorResponse>

    @ExceptionHandler(PurchaseOrderValidationException::class)
    fun handleValidation(ex: PurchaseOrderValidationException): ResponseEntity<ErrorResponse>

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse>
}
```

**Formato de erro:**
```json
{ "errors": [{ "code": "PURCHASE_ORDER_NOT_FOUND", "message": "Purchase order not found" }] }
```

### 4.7 Jackson Config

```kotlin
@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        registerModule(JavaTimeModule())
    }
}
```

---

## 5. Módulo `query/`

### 5.1 Schema de Query (PostgreSQL, per-tenant)

```sql
-- Tabela principal
CREATE TABLE purchase_order (
    id              VARCHAR(36) PRIMARY KEY,
    status          VARCHAR(20) NOT NULL,
    type            VARCHAR(20),
    customer        VARCHAR(36),
    coupon_code     VARCHAR(100),
    payment_mean    JSONB,
    payment_description TEXT,
    segmentation    JSONB,
    mgm_custom_fields JSONB,
    on_boarding_sale_custom_fields JSONB,
    coupon_custom_fields JSONB,
    callback        JSONB,
    reason          JSONB,
    security_code_informed JSONB,
    sales_force     JSONB,
    protocol        VARCHAR(100),
    subscription_id VARCHAR(36),
    channel_create  JSONB,
    channel_checkout JSONB,
    version         BIGINT NOT NULL DEFAULT 0,
    created         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_item (
    id              VARCHAR(36) PRIMARY KEY,
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id),
    catalog_offer_id VARCHAR(36) NOT NULL,
    price           JSONB NOT NULL,
    validity        JSONB,
    offer_items     JSONB,
    prices_per_period JSONB
);

CREATE TABLE payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id),
    type            VARCHAR(50),
    installments    INT,
    installment_value JSONB,
    total_value     JSONB,
    card_token      VARCHAR(100)
);

CREATE TABLE discounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id),
    percentage      DECIMAL(5,2),
    value_discount  JSONB,
    coupon          VARCHAR(100)
);

CREATE TABLE freight (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id) UNIQUE,
    price           JSONB NOT NULL,
    delivery_estimate_business_days INT,
    type            VARCHAR(50),
    address         JSONB,
    latitude        VARCHAR(30),
    longitude       VARCHAR(30)
);

CREATE TABLE customer_order (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id) UNIQUE,
    customer_order_id VARCHAR(36) NOT NULL
);

CREATE TABLE installation_attributes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id VARCHAR(36) NOT NULL REFERENCES purchase_order(id),
    product_type_id VARCHAR(100) NOT NULL,
    attributes      JSONB NOT NULL,
    UNIQUE (purchase_order_id, product_type_id)
);
```

### 5.2 Event Handlers (25)

Cada handler recebe o evento vindo do Kafka e projeta no query DB via JDBC:

```kotlin
@Component
class PurchaseOrderEventHandlers(private val jdbcTemplate: JdbcTemplate) {

    fun on(event: PurchaseOrderCreated) {
        jdbcTemplate.update(
            "INSERT INTO purchase_order (id, status, type, customer, callback, channel_create, created, updated, version) VALUES (?,?,?,?,?::jsonb,?::jsonb,now(),now(),1)",
            event.aggregateId.value, "OPENED", event.type?.name,
            event.customer?.id, toJson(event.callback), toJson(event.channel)
        )
    }

    fun on(event: PurchaseOrderItemAdded) { /* INSERT INTO order_item */ }
    fun on(event: PurchaseOrderItemRemoved) { /* DELETE FROM order_item */ }
    fun on(event: PurchaseOrderItemUpdated) { /* UPDATE order_item */ }
    fun on(event: PurchaseOrderPaymentUpdated) { /* UPSERT payment */ }
    fun on(event: PurchaseOrderFreightUpdated) { /* UPSERT freight */ }
    // ... 19 outros handlers
}
```

### 5.3 Query Repositories

```kotlin
@Repository
class PurchaseOrderQueryRepository(private val jdbcTemplate: JdbcTemplate) {
    fun findById(id: String, tenant: String): PurchaseOrderResponse?
    fun findByProtocol(protocol: String, tenant: String): PurchaseOrderResponse?
    fun findByCustomer(customerId: String, status: String?, start: String?, end: String?, tenant: String): List<PurchaseOrderResponse>
    fun getStatus(id: String, tenant: String): PurchaseOrderStatusResponse?
}
```

---

## 6. Módulo `command-app/`

### 6.1 Command Handlers (14)

```kotlin
@Service
class PurchaseOrderCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val validator: PurchaseOrderValidator,
    private val checkoutService: PurchaseOrderCheckoutService,
    private val eventPublisher: PurchaseOrderEventPublisher
) {
    @Transactional
    fun handle(command: CreatePurchaseOrderCommand): PurchaseOrderId {
        val po = PurchaseOrder.create(command.id, command.type, command.customer, command.callback, command.channel)
        repository.save(po)
        eventPublisher.publish(po)
        return po.id
    }

    @Transactional
    fun handle(command: CheckoutCommand): CustomerOrder {
        val po = repository.findByIdOrThrow(command.id)
        // CORREÇÃO de bug: validate() chamado apenas uma vez
        validator.validate(po)
        val customerOrder = checkoutService.checkout(po, command)
        po.applyChange(PurchaseOrderCheckedOut(po.id, customerOrder, command.channel, command.paymentSecurityCodes))
        repository.save(po)
        eventPublisher.publish(po)
        return customerOrder
    }

    // ... 12 outros handlers
}
```

**Handlers especializados (um por grupo de commands):**

| Handler | Commands |
|---|---|
| `PurchaseOrderCommandHandler` | Create, CreateCoupon, Delete, UpdateType, Validate, Find |
| `PurchaseOrderItemCommandHandler` | AddItem, UpdateItem, RemoveItem |
| `PurchaseOrderPaymentCommandHandler` | UpdatePayment |
| `PurchaseOrderFreightCommandHandler` | UpdateFreight |
| `PurchaseOrderCouponCommandHandler` | UpdateCoupon |
| `PurchaseOrderCustomerCommandHandler` | UpdateCustomer |
| `PurchaseOrderProtocolCommandHandler` | UpdateProtocol |
| `PurchaseOrderSubscriptionCommandHandler` | UpdateSubscription |
| `PurchaseOrderSegmentationCommandHandler` | UpdateSegmentation |
| `PurchaseOrderOnBoardingSaleCommandHandler` | UpdateOnBoardingSale |
| `PurchaseOrderMgmCommandHandler` | UpdateMgm, DeleteMgm |
| `PurchaseOrderSalesForceCommandHandler` | UpdateSalesForce, RemoveSalesForce |
| `PurchaseOrderInstallationAttributesCommandHandler` | UpdateInstallationAttributes, DeleteInstallationAttributes |
| `PurchaseOrderCustomerOrderCommandHandler` | UpdateCustomerOrder (callback COM) |

### 6.2 Controllers REST (implementam `PurchaseOrderCommandApi`)

```kotlin
@RestController
class PurchaseOrderCommandController(
    private val commandHandler: PurchaseOrderCommandHandler,
    // ... outros handlers
) : PurchaseOrderCommandApi {

    override fun create(request: PurchaseOrderRequest, ...): ResponseEntity<CreatePurchaseOrderResponse> {
        val id = commandHandler.handle(request.toCommand())
        return ResponseEntity.status(201).body(CreatePurchaseOrderResponse(id.value))
    }

    // Cada endpoint delega para o handler correspondente
}
```

### 6.3 Application Config

```yaml
# command-app/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: sales-manager-command
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/sales_manager}
    username: ${DB_USER:sm_user}
    password: ${DB_PASSWORD:sm_pass}
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

kafka:
  topic:
    purchase-events: rw_sm_purchase_events

feign:
  client:
    config:
      default:
        connect-timeout: 10000
        read-timeout: 60000

com:
  url: ${COM_URL}
cms:
  url: ${CMS_URL}
pcm:
  url: ${PCM_URL}
cim:
  url: ${CIM_URL}
coupon:
  url: ${COUPON_URL}
mgm:
  url: ${MGM_URL}

tenant:
  prefix: rw_sm
```

---

## 7. Módulo `query-app/`

### 7.1 Query Controllers (implementam `PurchaseOrderQueryApi`)

```kotlin
@RestController
class PurchaseOrderQueryController(
    private val repository: PurchaseOrderQueryRepository
) : PurchaseOrderQueryApi {

    override fun findById(purchaseOrderId: String, ...): ResponseEntity<PurchaseOrderResponse> {
        val response = repository.findById(purchaseOrderId, TenantContext.get())
            ?: throw PurchaseOrderNotFoundException(purchaseOrderId)
        return ResponseEntity.ok(response)
    }
    // ...
}
```

### 7.2 Kafka Consumer (Event Handler)

```kotlin
@Component
class PurchaseOrderEventConsumer(
    private val eventHandlers: PurchaseOrderEventHandlers,
    private val objectMapper: ObjectMapper
) {
    @KafkaListener(
        topics = ["\${kafka.topic.purchase-events}"],
        groupId = "\${kafka.consumer.group-id:sm-query-handler}"
    )
    fun receive(message: String, @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String) {
        val envelope = objectMapper.readValue(message, KafkaEventEnvelope::class.java)
        val event = EventTypeRegistry.resolve(envelope.header.eventType)
        eventHandlers.dispatch(event)
    }
}
```

### 7.3 Application Config

```yaml
# query-app/src/main/resources/application.yml
server:
  port: 8180

spring:
  application:
    name: sales-manager-query
  kafka:
    consumer:
      group-id: sm-query-handler
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
  jackson:
    default-property-inclusion: non_null
```

---

## 8. Módulo `consumer-app/`

### 8.1 Kafka Listener

```kotlin
@Component
class PurchaseOrderStatusConsumer(
    private val callbackService: CallbackService,
    private val queryRepository: PurchaseOrderQueryRepository
) {
    @KafkaListener(
        topics = ["\${kafka.topic.purchase-events}"],
        groupId = "\${kafka.consumer.group-id:sm-purchase-order-status}"
    )
    fun receive(message: String) {
        val envelope = objectMapper.readValue(message, KafkaEventEnvelope::class.java)
        val po = queryRepository.findById(envelope.event.purchaseOrder.id, TenantContext.get())
        if (po?.callback != null) {
            callbackService.notify(po.callback, envelope)
        }
    }
}
```

### 8.2 Callback Service

```kotlin
@Service
class CallbackService(private val callbackClient: CallbackFeignClient) {
    fun notify(callback: Callback, payload: Any) {
        // POST para callback.url com headers de callback.headers
        callbackClient.notify(callback.url, callback.headers, payload)
    }
}
```

### 8.3 Application Config

```yaml
# consumer-app/src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: sales-manager-consumer
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: sm-purchase-order-status
      auto-offset-reset: latest
```

---

## 9. Kafka: Formato de Mensagem

### 9.1 Envelope

```kotlin
data class KafkaEventEnvelope(
    val header: KafkaEventHeader,
    val event: KafkaEventPayload
)

data class KafkaEventHeader(
    val eventId: String,
    val eventType: String,          // "PurchaseOrderCreated" | "PurchaseOrderCheckedout" | "PurchaseOrderFinished"
    val timestamp: String,          // ISO 8601
    val domain: String,             // "SALES-MANAGER"
    val context: KafkaEventContext
)

data class KafkaEventContext(
    val organization: String,
    val application: String,
    val globalTrackingId: String?,
    val channel: String?
)

data class KafkaEventPayload(
    val purchaseOrder: PurchaseOrderSnapshot
)
```

**Nota de compatibilidade:** O campo `eventType` usa `PurchaseOrderCheckedout` (não `CheckedOut`) para manter compatibilidade com consumers legados.

### 9.2 Tipos de Evento Publicados

| Situação | eventType |
|---|---|
| status = OPENED | `PurchaseOrderCreated` |
| status = CHECKED_OUT | `PurchaseOrderCheckedout` |
| status = COMPLETED / FAILED / CANCELED | `PurchaseOrderFinished` |

### 9.3 Producer Config

```kotlin
@Service
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${kafka.topic.purchase-events}") private val topic: String,
    private val objectMapper: ObjectMapper
) : PurchaseOrderEventPublisher {

    override fun publish(purchaseOrder: PurchaseOrder) {
        val envelope = buildEnvelope(purchaseOrder)
        kafkaTemplate.send(topic, purchaseOrder.id.value, objectMapper.writeValueAsString(envelope))
    }

    private fun buildEnvelope(po: PurchaseOrder): KafkaEventEnvelope {
        val eventType = when (po.status) {
            OPENED -> "PurchaseOrderCreated"
            CHECKED_OUT -> "PurchaseOrderCheckedout"
            else -> "PurchaseOrderFinished"
        }
        return KafkaEventEnvelope(
            header = KafkaEventHeader(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType,
                timestamp = ZonedDateTime.now(ZoneOffset.UTC).toString(),
                domain = "SALES-MANAGER",
                context = KafkaEventContext(
                    organization = TenantContext.get(),
                    application = "rw_sm_c",
                    globalTrackingId = null,
                    channel = po.channelCreate?.type
                )
            ),
            event = KafkaEventPayload(purchaseOrder = po.toSnapshot())
        )
    }
}
```

---

## 10. Liquibase Migrations

### 10.1 Estrutura

```
infrastructure/src/main/resources/
└── db/
    └── changelog/
        ├── master.xml
        ├── event-store/
        │   └── 001-domain-events.xml
        │   └── 002-outbox.xml
        └── query/
            ├── 001-purchase-order.xml
            ├── 002-order-item.xml
            ├── 003-payment.xml
            ├── 004-discounts.xml
            ├── 005-freight.xml
            ├── 006-customer-order.xml
            └── 007-installation-attributes.xml
```

### 10.2 Execução per-tenant

O `LiquibaseHandler` é chamado em:
1. Startup da aplicação para tenants conhecidos
2. Primeira requisição de um novo tenant (lazy initialization via `TenantFilter`)

---

## 11. Testes

### 11.1 Estratégia por Módulo

| Módulo | Tipo | Ferramentas |
|---|---|---|
| `domain/` | Unitário | JUnit 5 + MockK |
| `api/` | Unitário (validadores) | JUnit 5 |
| `infrastructure/` | Integração | Testcontainers (PostgreSQL) |
| `query/` | Integração | Testcontainers (PostgreSQL + Kafka) |
| `command-app/` | Integração + Contract | MockMvc + WireMock |
| `query-app/` | Integração + Contract | MockMvc + Testcontainers |
| `consumer-app/` | Integração | Testcontainers (Kafka) |

### 11.2 Testes de Domínio (exemplos)

```kotlin
class PurchaseOrderTest {
    @Test
    fun `deve criar purchase order com status OPENED`() {
        val id = PurchaseOrderId()
        val po = PurchaseOrder.create(id, PurchaseOrderType.NORMAL, Customer("cust-1"), null, null)
        assertThat(po.status).isEqualTo(PurchaseOrderStatus.OPENED)
        assertThat(po.pendingEvents).hasSize(1)
        assertThat(po.pendingEvents.first()).isInstanceOf(PurchaseOrderCreated::class.java)
    }

    @Test
    fun `não deve transicionar para CHECKED_OUT sem estar OPENED`() {
        // ...
    }

    @Test
    fun `replay de eventos deve restaurar estado identico`() {
        val po = buildPurchaseOrderWithMultipleEvents()
        val events = po.pendingEvents.toList()
        val replayed = PurchaseOrder.empty()
        events.forEach { replayed.replayEvent(it) }
        assertThat(replayed).isEqualTo(po)
    }
}
```

### 11.3 Testes de Contract (API)

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderCommandApiTest {
    @Test
    fun `POST purchase-orders deve retornar 201 com id`() {
        mockMvc.post("/purchase-orders") {
            header("X-Realwave-Organization-Slug", "test-tenant")
            header("Authorization", "Bearer ${validToken}")
            contentType = APPLICATION_JSON
            content = """{"type":"NORMAL","customer":"cust-uuid"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNotEmpty() }
        }
    }
}
```

---

## 12. Correções de Bugs (em relação ao legado)

| Bug | Local no novo código | Correção |
|---|---|---|
| Dupla validação no checkout | `PurchaseOrderCommandHandler.handle(CheckoutCommand)` | Chamar `validator.validate(po)` uma única vez |
| Coupon checkout hardcoded `items[0].offerItems[0]` | `PurchaseOrderCouponCheckoutService` | Iterar todos os items e todos os offerItems |
| Status `FAILED` nunca usado | `PurchaseOrderCustomerOrderCommandHandler` | Transicionar para `FAILED` quando COM retorna status FAILED |
| `Checkedout` vs `CheckedOut` no Kafka | `KafkaEventPublisher` | Manter `PurchaseOrderCheckedout` (sem maiúscula em 'o') para compatibilidade |

---

## 13. Headers HTTP

### 13.1 Headers Obrigatórios (todas as requisições)

| Header | Descrição |
|---|---|
| `X-Realwave-Organization-Slug` | Identifica o tenant (ex: `my-company`) |
| `Authorization: Bearer <JWT>` | Token Keycloak |

### 13.2 Headers Opcionais

| Header | Descrição |
|---|---|
| `X-Realwave-Tracking-Id` | ID de rastreamento global |
| `X-Realwave-Tracking-Context` | Contexto de rastreamento |
| `X-Realwave-Channel` | Canal da requisição (`WEB`, `MOBILE`, `SALES_FORCE`) |
| `X-Realwave-Application-Id` | Identificador da aplicação |

### 13.3 Propagação via Feign

```kotlin
@Component
class TenantFeignInterceptor : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.header("X-Realwave-Organization-Slug", TenantContext.get())
        // propagar tracking id se presente
    }
}
```

---

## 14. Variáveis de Ambiente

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/sales_manager` | JDBC URL do PostgreSQL |
| `DB_USER` | `sm_user` | Usuário do banco |
| `DB_PASSWORD` | `sm_pass` | Senha do banco |
| `KEYCLOAK_ISSUER_URI` | — | Issuer URI do Keycloak (obrigatório) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Endereços Kafka |
| `COM_URL` | — | URL do Customer Order Manager |
| `CMS_URL` | — | URL do Catalog Manager Service |
| `PCM_URL` | — | URL do Product Catalog Manager |
| `CIM_URL` | — | URL do Customer Info Manager |
| `COUPON_URL` | — | URL do Coupon Service |
| `MGM_URL` | — | URL do MGM Service |

---

## 15. Actuator e Observabilidade

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

```kotlin
// Dependências
implementation("io.micrometer:micrometer-registry-prometheus")
implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
```

---

## Referência Rápida de Arquivos

| Documento | Localização |
|---|---|
| Análise completa do domínio | `legacy_sales_manager/analise-completa.md` |
| 25 endpoints de comando (JSON exato) | `legacy_sales_manager/docs/api-contracts/command-endpoints.md` |
| 4 endpoints de query (JSON exato) | `legacy_sales_manager/docs/api-contracts/query-endpoints.md` |
| Headers, multi-tenancy, OAuth2 | `legacy_sales_manager/docs/api-contracts/security.md` |
| 25 eventos (data classes Kotlin) | `legacy_sales_manager/docs/events/domain-events.md` |
| Kafka envelope e configuração | `legacy_sales_manager/docs/events/kafka-config.md` |
| Plano de migração e fases | `docs/migration-plan.md` |
