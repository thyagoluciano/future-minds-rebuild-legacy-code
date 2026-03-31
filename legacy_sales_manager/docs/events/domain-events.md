# Eventos de Domínio — Contratos

> Pacote base: `br.com.zup.realwave.sales.manager.domain.event`  
> Todos herdam de `PurchaseOrderApplicableEvent` que implementa `Event` (Event Sourcing)  
> Cada evento implementa `apply(purchaseOrder: PurchaseOrder)` que muta o estado do aggregate

---

## Hierarquia

```
Event (interface — event-sourcing-core)
  └── PurchaseOrderApplicableEvent (abstract)
        ├── PurchaseOrderCreated
        ├── PurchaseOrderCheckedOut
        ├── PurchaseOrderDeleted
        ├── PurchaseOrderStatusUpdated
        ├── PurchaseOrderReasonStatusUpdated
        ├── PurchaseOrderTypeUpdated
        ├── PurchaseOrderItemAdded
        ├── PurchaseOrderItemRemoved
        ├── PurchaseOrderItemUpdated
        ├── PurchaseOrderPaymentUpdated
        ├── PurchaseOrderFreightUpdated
        ├── PurchaseOrderCouponUpdated
        ├── PurchaseOrderCustomerUpdated
        ├── PurchaseOrderCustomerOrderUpdated
        ├── PurchaseOrderMgmUpdated
        ├── PurchaseOrderMgmDeleted
        ├── PurchaseOrderSegmentationUpdated
        ├── PurchaseOrderOnBoardingSaleUpdated
        ├── PurchaseOrderSalesForceUpdated
        ├── PurchaseOrderSalesForceRemoved
        ├── PurchaseOrderSubscriptionUpdated
        ├── PurchaseOrderProtocolUpdated
        ├── PurchaseOrderInstallationAttributesUpdated
        └── PurchaseOrderInstallationAttributesDeleted
```

---

## Tipos Base (usados em múltiplos eventos)

### Price
```kotlin
data class Price(
    val currency: String,   // ex: "BRL"
    val amount: Int,        // em centavos: 9990 = R$99,90
    val scale: Int          // casas decimais: 2
)
```

### Customer
```kotlin
data class Customer(val id: String)
```

### Channel
```kotlin
data class Channel(val value: String?)   // "WEB", "MOBILE", "SALES_FORCE", etc.
```

### Callback
```kotlin
data class Callback(
    val url: String,
    val headers: JsonNode?
)
```

### Reason
```kotlin
data class Reason(
    val code: String?,
    val description: String?
)
```

### Item (complexo)
```kotlin
data class Item(
    val id: Id,                              // UUID gerado
    val catalogOfferId: CatalogOfferId,      // {value: String}
    val catalogOfferType: CatalogOfferType,  // {value: String}
    val price: Price,
    val validity: OfferValidity,             // {period: String?, duration: Int?, unlimited: Boolean}
    val offerFields: OfferFields,            // {value: JsonNode?}
    val customFields: CustomFields,          // {value: JsonNode?}
    val offerItems: List<OfferItem>,
    val pricesPerPeriod: List<PricePerPeriod> = listOf(),
    val quantity: Quantity                   // {value: Int, default: 1}
)

data class OfferItem(
    val productId: ProductId?,               // {value: String}
    val catalogOfferItemId: CatalogOfferItemId,
    val price: Price,
    val recurrent: Boolean = false,
    val customFields: CustomFields?,
    val userParameters: Map<String, Any>?
)

data class PricePerPeriod(
    val totalPrice: Price,
    val totalDiscountPrice: Price,
    val totalPriceWithDiscount: Price,
    val startAt: Int,
    val endAt: Int,
    val items: List<PricePerPeriodItem>
)
```

### CustomerOrder
```kotlin
data class CustomerOrder(
    val customerOrderId: String?,
    val status: String?,
    val steps: List<Step>?,
    val boleto: Boleto?
)

data class Step(
    val step: String?,
    val status: String?,
    val startedAt: String?,
    val endedAt: String?,
    val processed: Int?,
    val total: Int?
)

data class Boleto(
    val methodId: String?,
    val payload: JsonNode?
)
```

### SecurityCodeInformed
```kotlin
data class SecurityCodeInformed(
    val methodId: String,
    val securityCodeInformed: Boolean
)
```

---

## Eventos — Detalhamento

### 1. PurchaseOrderCreated

**Trigger:** `CreatePurchaseOrderCommand`  
**Efeito:** `status = OPENED`

```kotlin
data class PurchaseOrderCreated(
    val aggregateId: AggregateId,           // ID do PurchaseOrder
    val purchaseOrderType: PurchaseOrderType?, // {value: String?} ex: "NORMAL", "COUPON"
    val callback: Callback?,
    val customer: Customer?
)
```

**JSON serializado:**
```json
{
  "aggregateId": "po-uuid-123",
  "purchaseOrderType": { "value": "NORMAL" },
  "callback": { "url": "https://...", "headers": null },
  "customer": { "id": "cust-uuid-123" }
}
```

---

### 2. PurchaseOrderCheckedOut

**Trigger:** `CheckoutCommand`  
**Efeito:** `status = CHECKED_OUT`, define `customerOrder` e `channelCheckout`

```kotlin
data class PurchaseOrderCheckedOut(
    val aggregateId: AggregateId,
    val customerOrder: CustomerOrder?,
    val channel: Channel,
    val securityCodeInformed: List<SecurityCodeInformed>
)
```

**JSON serializado:**
```json
{
  "aggregateId": "po-uuid-123",
  "customerOrder": {
    "customerOrderId": "com-order-uuid",
    "status": "PROCESSING",
    "steps": [],
    "boleto": null
  },
  "channel": { "value": "WEB" },
  "securityCodeInformed": [
    { "methodId": "card-uuid-333", "securityCodeInformed": true }
  ]
}
```

---

### 3. PurchaseOrderDeleted

**Trigger:** `DeletePurchaseOrderCommand`  
**Efeito:** `status = DELETED`

```kotlin
data class PurchaseOrderDeleted(
    val aggregateId: AggregateId
)
```

---

### 4. PurchaseOrderStatusUpdated

**Trigger:** Callbacks do COM (mudança de status pós-checkout)  
**Efeito:** muta `status`

```kotlin
data class PurchaseOrderStatusUpdated(
    val aggregateId: AggregateId,
    val status: PurchaseOrderStatus    // enum: OPENED, CHECKED_OUT, COMPLETED, FAILED, CANCELED, DELETED
)
```

---

### 5. PurchaseOrderReasonStatusUpdated

**Trigger:** `UpdateCustomerOrderCommand` quando há razão de falha  
**Efeito:** define `reason`

```kotlin
data class PurchaseOrderReasonStatusUpdated(
    val aggregateId: AggregateId,
    val reason: Reason?    // {code: String?, description: String?}
)
```

---

### 6. PurchaseOrderTypeUpdated

**Trigger:** `UpdatePurchaseOrderType`  
**Efeito:** muta `type`

```kotlin
data class PurchaseOrderTypeUpdated(
    val aggregateId: AggregateId,
    val purchaseOrderType: PurchaseOrderType?   // {value: String?}
)
```

---

### 7. PurchaseOrderItemAdded

**Trigger:** `AddItemCommand`  
**Efeito:** adiciona item ao `items`

```kotlin
data class PurchaseOrderItemAdded(
    val aggregateId: AggregateId,
    val item: Item
)
```

**JSON serializado (exemplo):**
```json
{
  "aggregateId": "po-uuid-123",
  "item": {
    "id": { "value": "item-uuid-gerado" },
    "catalogOfferId": { "value": "offer-uuid-456" },
    "catalogOfferType": { "value": "OFFER" },
    "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
    "validity": { "period": "MONTHLY", "duration": 12, "unlimited": false },
    "offerFields": { "value": null },
    "customFields": { "value": null },
    "offerItems": [
      {
        "productId": { "value": "prod-uuid-789" },
        "catalogOfferItemId": { "value": "item-uuid-111" },
        "price": { "currency": "BRL", "amount": 4995, "scale": 2 },
        "recurrent": false,
        "customFields": null,
        "userParameters": null
      }
    ],
    "pricesPerPeriod": [],
    "quantity": { "value": 1 }
  }
}
```

---

### 8. PurchaseOrderItemRemoved

**Trigger:** `RemoveItemCommand`  
**Efeito:** remove item do `items` pelo ID

```kotlin
data class PurchaseOrderItemRemoved(
    val aggregateId: AggregateId,
    val itemId: Item.Id    // {value: String}
)
```

---

### 9. PurchaseOrderItemUpdated

**Trigger:** `UpdateItemCommand`  
**Efeito:** remove o item antigo e adiciona o novo (replace by id)

```kotlin
data class PurchaseOrderItemUpdated(
    val aggregateId: AggregateId,
    val item: Item
)
```

---

### 10. PurchaseOrderPaymentUpdated

**Trigger:** `UpdatePaymentCommand`  
**Efeito:** substitui `payment`

```kotlin
data class PurchaseOrderPaymentUpdated(
    val aggregateId: AggregateId,
    val payment: Payment
)

data class Payment(
    val methods: List<PaymentMethod>,
    val description: Description?,    // {value: String}
    val async: Boolean?
)

data class PaymentMethod(
    val method: String,               // CREDIT_CARD, DEBIT_CARD, BOLETO, REWARD, etc.
    val methodId: String?,
    val installments: Int?,
    val price: Price?,
    val customFields: JsonNode?,
    val securityCodeInformed: Boolean
)
```

---

### 11. PurchaseOrderFreightUpdated

**Trigger:** `UpdateFreightCommand`  
**Efeito:** substitui `freight`

```kotlin
data class PurchaseOrderFreightUpdated(
    val aggregateId: AggregateId,
    val freight: Freight
)

data class Freight(
    val address: Address,
    val price: Price,
    val type: Type,                    // {value: String}
    val deliveryTotalTime: DeliveryTotalTime   // {value: Int}
)

data class Address(
    val city: String,
    val complement: String?,
    val country: String,
    val district: String,
    val name: String,
    val state: String,
    val street: String,
    val zipCode: String,
    val number: String
)
```

---

### 12. PurchaseOrderCouponUpdated

**Trigger:** `UpdateCouponCommand` / `CreatePurchaseOrderCouponCommand`  
**Efeito:** substitui `coupon`

```kotlin
data class PurchaseOrderCouponUpdated(
    val aggregateId: AggregateId,
    val coupon: CouponCode
)

data class CouponCode(
    val code: String,
    val customFields: JsonNode?,
    val description: String?,
    val reward: Reward?
)

data class Reward(
    val type: String,
    val discounts: List<Discount>
)

data class Discount(
    val segment: Segment?,
    val discount: Price?,
    val discountAsPercent: Int?
)

data class Segment(
    val id: String,
    val name: String,
    val type: String
)
```

---

### 13. PurchaseOrderCustomerUpdated

**Trigger:** `UpdateCustomerCommand`  
**Efeito:** substitui `customer`

```kotlin
data class PurchaseOrderCustomerUpdated(
    val aggregateId: AggregateId,
    val customer: Customer    // {id: String}
)
```

---

### 14. PurchaseOrderCustomerOrderUpdated

**Trigger:** `UpdateCustomerOrderCommand` (callback do COM)  
**Efeito:** atualiza `customerOrder`

```kotlin
data class PurchaseOrderCustomerOrderUpdated(
    val aggregateId: AggregateId,
    val customerOrder: CustomerOrder
)
```

---

### 15. PurchaseOrderMgmUpdated

**Trigger:** `UpdateMgmCommand`  
**Efeito:** substitui `mgm`

```kotlin
data class PurchaseOrderMgmUpdated(
    val aggregateId: AggregateId,
    val mgm: Mgm
)

data class Mgm(
    val code: String,
    val customFields: JsonNode?
)
```

---

### 16. PurchaseOrderMgmDeleted

**Trigger:** `DeleteMgmCommand`  
**Efeito:** define `mgm = null`

```kotlin
data class PurchaseOrderMgmDeleted(
    val aggregateId: AggregateId,
    val mgm: Mgm?
)
```

---

### 17. PurchaseOrderSegmentationUpdated

**Trigger:** `UpdateSegmentationCommand`  
**Efeito:** substitui `segmentation`

```kotlin
data class PurchaseOrderSegmentationUpdated(
    val aggregateId: AggregateId,
    val segmentation: Segmentation    // {query: JsonNode}
)
```

---

### 18. PurchaseOrderOnBoardingSaleUpdated

**Trigger:** `UpdateOnBoardingSaleCommand`  
**Efeito:** substitui `onBoardingSale`

```kotlin
data class PurchaseOrderOnBoardingSaleUpdated(
    val aggregateId: AggregateId,
    val onBoardingSale: OnBoardingSale
)

data class OnBoardingSale(
    val offer: CatalogOfferId,      // {value: String}
    val customFields: JsonNode?
)
```

---

### 19. PurchaseOrderSalesForceUpdated

**Trigger:** `UpdateSalesForceCommand`  
**Efeito:** substitui `salesForce`

```kotlin
data class PurchaseOrderSalesForceUpdated(
    val aggregateId: AggregateId,
    val salesForce: SalesForce
)

data class SalesForce(
    val id: String,
    val name: String
)
```

---

### 20. PurchaseOrderSalesForceRemoved

**Trigger:** `RemoveSalesForceCommand`  
**Efeito:** define `salesForce = null`

```kotlin
data class PurchaseOrderSalesForceRemoved(
    val aggregateId: AggregateId,
    val salesForce: SalesForce?
)
```

---

### 21. PurchaseOrderSubscriptionUpdated

**Trigger:** `UpdateSubscriptionCommand`  
**Efeito:** define `subscriptionId = subscription.toString()`

```kotlin
data class PurchaseOrderSubscriptionUpdated(
    val aggregateId: AggregateId,
    val subscription: Subscription    // {id: String}
)
```

---

### 22. PurchaseOrderProtocolUpdated

**Trigger:** `UpdateProtocolCommand`  
**Efeito:** define `protocol = protocol.value`

```kotlin
data class PurchaseOrderProtocolUpdated(
    val aggregateId: AggregateId,
    val protocol: Protocol    // {value: String}
)
```

---

### 23. PurchaseOrderInstallationAttributesUpdated

**Trigger:** `UpdateInstallationAttributesCommand`  
**Efeito:** adiciona/atualiza entrada em `installationAttributes[productTypeId]`

```kotlin
data class PurchaseOrderInstallationAttributesUpdated(
    val aggregateId: AggregateId,
    val installationAttribute: InstallationAttribute
)

data class InstallationAttribute(
    val productTypeId: ProductTypeId,    // {value: String}
    val attributes: Map<String, Any>
)
```

---

### 24. PurchaseOrderInstallationAttributesDeleted

**Trigger:** `DeleteInstallationAttributesCommand`  
**Efeito:** remove entrada em `installationAttributes[productTypeId]`

```kotlin
data class PurchaseOrderInstallationAttributesDeleted(
    val aggregateId: AggregateId,
    val productTypeId: ProductTypeId    // {value: String}
)
```

---

## Mapeamento: Command → Evento → Efeito no Aggregate

| Command | Evento Emitido | Efeito em PurchaseOrder |
|---------|---------------|------------------------|
| `CreatePurchaseOrderCommand` | `PurchaseOrderCreated` | status=OPENED, type, customer, callback |
| `CreatePurchaseOrderCouponCommand` | `PurchaseOrderCreated` | idem |
| `DeletePurchaseOrderCommand` | `PurchaseOrderDeleted` | status=DELETED |
| `UpdatePurchaseOrderType` | `PurchaseOrderTypeUpdated` | type |
| `CheckoutCommand` | `PurchaseOrderCheckedOut` | status=CHECKED_OUT, customerOrder, channelCheckout |
| `UpdateCustomerOrderCommand` | `PurchaseOrderCustomerOrderUpdated` + `PurchaseOrderStatusUpdated`/`PurchaseOrderReasonStatusUpdated` | customerOrder, status, reason |
| `AddItemCommand` | `PurchaseOrderItemAdded` | items.add(item) |
| `UpdateItemCommand` | `PurchaseOrderItemUpdated` | items.replace(item) |
| `RemoveItemCommand` | `PurchaseOrderItemRemoved` | items.remove(itemId) |
| `UpdatePaymentCommand` | `PurchaseOrderPaymentUpdated` | payment |
| `UpdateFreightCommand` | `PurchaseOrderFreightUpdated` | freight |
| `UpdateCouponCommand` | `PurchaseOrderCouponUpdated` | coupon |
| `UpdateCustomerCommand` | `PurchaseOrderCustomerUpdated` | customer |
| `UpdateMgmCommand` | `PurchaseOrderMgmUpdated` | mgm |
| `DeleteMgmCommand` | `PurchaseOrderMgmDeleted` | mgm=null |
| `UpdateSegmentationCommand` | `PurchaseOrderSegmentationUpdated` | segmentation |
| `UpdateOnBoardingSaleCommand` | `PurchaseOrderOnBoardingSaleUpdated` | onBoardingSale |
| `UpdateSalesForceCommand` | `PurchaseOrderSalesForceUpdated` | salesForce |
| `RemoveSalesForceCommand` | `PurchaseOrderSalesForceRemoved` | salesForce=null |
| `UpdateSubscriptionCommand` | `PurchaseOrderSubscriptionUpdated` | subscriptionId |
| `UpdateProtocolCommand` | `PurchaseOrderProtocolUpdated` | protocol |
| `UpdateInstallationAttributesCommand` | `PurchaseOrderInstallationAttributesUpdated` | installationAttributes[ptid] |
| `DeleteInstallationAttributesCommand` | `PurchaseOrderInstallationAttributesDeleted` | installationAttributes.remove(ptid) |
| `ValidatePurchaseOrder` | *(nenhum evento)* | sem mutação |
| `FindPurchaseOrderCommand` | *(nenhum evento)* | sem mutação |
