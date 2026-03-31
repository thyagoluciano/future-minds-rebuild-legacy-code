# Kafka — Configuração e Formato de Mensagem

---

## 1. Tópico

| Propriedade | Valor |
|-------------|-------|
| **Nome do tópico** | `rw_sm_purchase_events` |
| **Propriedade** | `kafka.topic.rw.sales.manager.events=rw_sm_purchase_events` |
| **Domínio** | `SALES-MANAGER` |

---

## 2. Producer

**Classe:** `PurchaseOrderKafkaProducer`  
**Módulo:** `realwave-sales-manager-producer`

### Serialização
- **Key Serializer:** `StringSerializer`
- **Value Serializer:** `StringSerializer`
- **Key:** `purchaseOrderId` (String/UUID)
- **Value:** JSON String (envelope completo)

### Quando é acionado
O producer é chamado após cada operação que **muda o status** do `PurchaseOrder`:

```kotlin
fun notifyPurchaseOrderStateUpdated(purchaseOrder: PurchaseOrder) {
    val eventType = when (purchaseOrder.status) {
        OPENED      -> "PurchaseOrderCreated"
        CHECKED_OUT -> "PurchaseOrderCheckedout"
        else        -> "PurchaseOrderFinished"
    }
    notify(purchaseOrder.idAsString(), eventType, purchaseOrder.toStateChange())
}
```

**Operações que disparam notificação:**
- `CreatePurchaseOrderCommand` → status OPENED → `PurchaseOrderCreated`
- `CreatePurchaseOrderCouponCommand` → status OPENED → `PurchaseOrderCreated`
- `CheckoutCommand` → status CHECKED_OUT → `PurchaseOrderCheckedout`
- `UpdateCustomerOrderCommand` (callback COM) → qualquer outro status → `PurchaseOrderFinished`

---

## 3. Formato da Mensagem (Envelope Kafka)

O envelope é serializado como JSON String. Estrutura completa:

```json
{
  "header": {
    "eventId": "uuid-evento",
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
  },
  "event": {
    "purchaseOrder": {
      "purchaseOrderId": "po-uuid-123",
      "status": "OPENED",
      "type": "NORMAL",
      "protocol": null,
      "subscriptionId": null,
      "channelCreate": { "value": "WEB" },
      "channelCheckout": null,
      "createdAt": "2026-03-30T10:00:00.000+0000",
      "updatedAt": null,
      "customer": { "id": "cust-uuid-123" },
      "callback": { "url": "https://system/callback", "headers": null },
      "reason": null,
      "segmentation": null,
      "mgm": null,
      "salesForce": null,
      "onBoardingSale": null,
      "coupon": null,
      "totalPrice": null,
      "payment": {
        "methods": [],
        "description": null
      },
      "freight": null,
      "items": [],
      "installationAttributes": [],
      "customerOrder": null,
      "securityCodeInformed": null
    }
  }
}
```

### Tipos de `eventType` por Status

| `purchaseOrder.status` | `eventType` no header |
|------------------------|----------------------|
| `OPENED` | `PurchaseOrderCreated` |
| `CHECKED_OUT` | `PurchaseOrderCheckedout` *(sem 'd'!)* |
| `COMPLETED`, `FAILED`, `CANCELED`, `DELETED` | `PurchaseOrderFinished` |

> **Atenção:** `PurchaseOrderCheckedout` é escrito sem o 'd' final — diferente do nome da classe do evento de domínio (`PurchaseOrderCheckedOut`).

---

## 4. Estrutura do Payload (`PurchaseOrderChangeEvent`)

O `event.purchaseOrder` dentro do envelope é uma snapshot completa do estado do `PurchaseOrder` no momento da notificação:

```kotlin
data class PurchaseOrderChangeEvent(val event: Event) {
    data class Event(val purchaseOrder: PurchaseOrder)
}

data class PurchaseOrder(
    var purchaseOrderId: String,
    var segmentation: Segmentation?,
    var onBoardingSale: OnBoardingSale?,
    var mgm: Mgm?,
    var customer: Customer?,
    var coupon: CouponCode?,
    var totalPrice: Price?,
    var payment: Payment,
    var customerOrder: CustomerOrder?,
    var installationAttributes: List<InstallationAttribute>,
    var items: MutableSet<Item>,
    var createdAt: String?,
    var updatedAt: String?,
    var protocol: String?,
    var type: String?,
    var subscriptionId: String?,
    var channelCreate: Channel?,
    var channelCheckout: Channel?,
    var callback: Callback?,
    var reason: Reason?,
    var securityCodeInformed: List<SecurityCodeInformed>?,
    var status: PurchaseOrderStatus,    // enum
    var salesForce: SalesForce?
)
```

**Enum `PurchaseOrderStatus`** (no módulo events):
```kotlin
enum class PurchaseOrderStatus {
    OPENED, CHECKED_OUT, COMPLETED, FAILED, CANCELED, DELETED
}
```

---

## 5. Consumer

**Classe:** `PurchaseOrderConsumer`  
**Módulo:** `realwave-sales-manager-consumer`  
**Porta:** 8082

### Configuração

```properties
# Tópico
kafka.topic.rw.sales.manager.events=rw_sm_purchase_events

# Consumer Group
spring.kafka.consumer.group-id=sm-purchase-order-status
sm.kafka.purchase.order.status.group-id=sm-purchase-order-status

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.listener.concurrency=2

# Key/Value Deserializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

### Anotação do listener
```kotlin
@KafkaListener(
    topics = ["\${kafka.topic.rw.sales.manager.events}"],
    groupId = "\${sm.kafka.purchase.order.status.group-id}"
)
fun receive(
    @Header(RECEIVED_TOPIC) topic: String,
    @Header(RECEIVED_PARTITION_ID) partition: Int,
    @Header(RECEIVED_MESSAGE_KEY) key: String,
    message: String
)
```

### Lógica de processamento
```
1. Recebe mensagem String (JSON)
2. ParseEventUtils.extractEvent<PurchaseOrderChangeEvent>(message)
3. ParseEventUtils.loadContextVariables(message) → restaura RealwaveContext
4. Extrai purchaseOrderId do evento
5. Busca PurchaseOrder no repositório
6. Se callback != null:
   → CallbackService.notify(purchaseOrder)
   → HTTP POST para callback.url com estado atual
   → Inclui callback.headers customizados
```

> **Nota importante:** O consumer **não** atualiza o Query DB. Ele apenas dispara callbacks externos. A atualização do Query DB é responsabilidade do `query-event-handler` (que ouve diretamente o Event Store, não o Kafka).

---

## 6. Configurações Kafka por Módulo

### command-application (porta 8080)
```properties
# Bootstrap
spring.kafka.bootstrap-servers=localhost:9092

# Producer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

### consumer (porta 8082)
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=sm-purchase-order-status
sm.kafka.purchase.order.status.group-id=sm-purchase-order-status
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.listener.concurrency=2
```

---

## 7. Fluxo Completo Kafka

```
Command Handler (porta 8080)
  │ salva evento no Event Store
  │ chama purchaseOrderProducer.notifyPurchaseOrderStateUpdated()
  ▼
PurchaseOrderKafkaProducer
  │ determina eventType por status
  │ constrói envelope JSON com header + event (snapshot)
  ▼
Kafka Topic: rw_sm_purchase_events
  │
  ├──────────────────────────────────────────┐
  ▼                                          ▼
PurchaseOrderConsumer                  (Query Event Handler
(grupo: sm-purchase-order-status)       ouve Event Store
  │                                     diretamente, não Kafka)
  ▼
CallbackService.notify()
  → POST {callback.url}
    body: estado atual do PurchaseOrder
    headers: customizados configurados no pedido
```

---

## 8. Exemplo de Mensagem Completa — Checkout

Mensagem publicada após `POST /purchase-orders/{id}/checkout`:

```json
{
  "header": {
    "eventId": "evt-uuid-789",
    "eventType": "PurchaseOrderCheckedout",
    "timestamp": "2026-03-30T11:30:00",
    "domain": "SALES-MANAGER",
    "context": {
      "organization": "minha-empresa",
      "application": "rw_sm_c",
      "globalTrackingId": "track-uuid",
      "contextTrackingId": "ctx-uuid",
      "channel": "WEB"
    }
  },
  "event": {
    "purchaseOrder": {
      "purchaseOrderId": "po-uuid-123",
      "status": "CHECKED_OUT",
      "type": "NORMAL",
      "customer": { "id": "cust-uuid-123" },
      "channelCreate": { "value": "WEB" },
      "channelCheckout": { "value": "WEB" },
      "payment": {
        "methods": [
          {
            "method": "CREDIT_CARD",
            "methodId": "card-uuid-333",
            "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
            "installments": 3,
            "customFields": null,
            "securityCodeInformed": true
          }
        ],
        "description": null
      },
      "customerOrder": {
        "customerOrderId": "com-order-uuid",
        "status": "PROCESSING",
        "steps": [],
        "boleto": null
      },
      "items": [
        {
          "id": { "value": "item-uuid-gerado" },
          "catalogOfferId": { "value": "offer-uuid-456" },
          "catalogOfferType": { "value": "OFFER" },
          "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
          "quantity": { "value": 1 }
        }
      ],
      "securityCodeInformed": [
        { "methodId": "card-uuid-333", "securityCodeInformed": true }
      ],
      "callback": { "url": "https://system/callback", "headers": null },
      "createdAt": "2026-03-30T10:00:00.000+0000",
      "updatedAt": "2026-03-30T11:30:00.000+0000",
      "installationAttributes": [],
      "segmentation": null,
      "mgm": null,
      "salesForce": null,
      "onBoardingSale": null,
      "coupon": null,
      "totalPrice": null,
      "freight": null,
      "reason": null,
      "protocol": null,
      "subscriptionId": null
    }
  }
}
```
