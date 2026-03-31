# API Contracts — Query Endpoints (Leitura)

> Porta: **8180**  
> Base path: `/purchase-orders`  
> Content-Type: `application/json;charset=UTF-8`  
> Autenticação: Bearer Token (Keycloak) + headers de tenant (ver `security.md`)

---

## Estrutura Completa: `PurchaseOrderResponse`

Retornada pelos endpoints GET. Todos os campos marcados com `?` são opcionais/nulos.

```json
{
  "id": "po-uuid-123",
  "type": "NORMAL",
  "protocol": "PROTO-2026-001",
  "subscriptionId": "sub-uuid-666",
  "status": "CHECKED_OUT",
  "channelCreate": "WEB",
  "channelCheckout": "MOBILE",
  "createdAt": "2026-03-30T10:00:00.000+0000",
  "updatedAt": "2026-03-30T11:30:00.000+0000",

  "customer": {
    "id": "cust-uuid-123"
  },

  "callback": {
    "url": "https://system/callback",
    "headers": { "X-Api-Key": "abc" }
  },

  "reason": {
    "code": "PAYMENT_FAILED",
    "description": "Cartão recusado"
  },

  "segmentation": { "query": { "field": "value" } },

  "mgm": {
    "code": "MGM-CODE-XYZ",
    "fields": { "key": "value" }
  },

  "salesForce": {
    "id": "sf-uuid-555",
    "name": "João Vendedor"
  },

  "onBoardingSale": {
    "id": "offer-uuid-onboarding",
    "fields": { "key": "value" }
  },

  "coupon": {
    "id": "PROMO10",
    "fields": { "key": "value" }
  },

  "totalPrice": {
    "currency": "BRL",
    "amount": 9990,
    "scale": 2
  },

  "discount": {
    "fullPrice": { "currency": "BRL", "amount": 9990, "scale": 2 },
    "discountValue": { "currency": "BRL", "amount": 999, "scale": 2 },
    "discountType": "PERCENT",
    "finalPrice": { "currency": "BRL", "amount": 8991, "scale": 2 }
  },

  "payment": {
    "methods": [
      {
        "method": "CREDIT_CARD",
        "methodId": "card-uuid-333",
        "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
        "customFields": {},
        "securityCodeInformed": true,
        "installments": 3
      }
    ],
    "description": {
      "value": "Pagamento mensal"
    }
  },

  "freight": {
    "type": "NORMAL",
    "price": { "currency": "BRL", "amount": 1000, "scale": 2 },
    "address": {
      "city": "São Paulo",
      "state": "SP",
      "country": "BR",
      "district": "Centro",
      "name": "Casa",
      "street": "Rua X",
      "zipCode": "01310-100",
      "number": "100",
      "complement": "Ap 1"
    },
    "deliveryTotalTime": 7
  },

  "items": [
    {
      "id": "item-uuid-gerado",
      "catalogOfferId": "offer-uuid-456",
      "catalogOfferType": "OFFER",
      "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
      "validity": {
        "period": "MONTHLY",
        "duration": 12,
        "unlimited": false
      },
      "customFields": {},
      "offerItems": [
        {
          "productId": "prod-uuid-789",
          "catalogOfferItemId": "item-uuid-111",
          "price": { "currency": "BRL", "amount": 4995, "scale": 2 },
          "customFields": {},
          "recurrent": false,
          "userParameters": { "key": "value" }
        }
      ],
      "pricesPerPeriod": [
        {
          "totalPrice": { "currency": "BRL", "amount": 9990, "scale": 2 },
          "totalDiscountPrice": { "currency": "BRL", "amount": 0, "scale": 2 },
          "totalPriceWithDiscount": { "currency": "BRL", "amount": 9990, "scale": 2 },
          "startAt": 0,
          "endAt": 11,
          "items": [
            {
              "compositionId": "comp-uuid-222",
              "itemId": "item-uuid-111",
              "price": { "currency": "BRL", "amount": 9990, "scale": 2 },
              "discountPrice": { "currency": "BRL", "amount": 0, "scale": 2 },
              "priceWithDiscount": { "currency": "BRL", "amount": 9990, "scale": 2 }
            }
          ]
        }
      ],
      "quantity": 1
    }
  ],

  "installationAttributes": [
    {
      "productTypeId": "ptype-uuid-444",
      "attributes": { "speed": "100mbps", "technology": "FIBER" }
    }
  ],

  "customerOrder": {
    "customerOrderId": "com-order-uuid",
    "status": "FINISHED",
    "steps": [
      {
        "step": "PAYMENT",
        "status": "COMPLETED",
        "startedAt": "2026-03-30T10:00:00",
        "endedAt": "2026-03-30T10:01:00"
      }
    ]
  }
}
```

---

## 1. Buscar Pedido por ID

**`GET /purchase-orders/{purchaseOrderId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Response Body
`PurchaseOrderResponse` (estrutura completa acima)

Lança `NotFoundException` (404) se não encontrado.

---

## 2. Buscar Pedido por Protocolo

**`GET /purchase-orders/{protocol}/protocol`** → `200 OK`

### Path Variables
- `protocol`: String, obrigatório — número do protocolo

### Response Body
`PurchaseOrderResponse` (estrutura completa acima)

Lança `NotFoundException` (404) se não encontrado.

---

## 3. Buscar Status do Pedido

**`GET /purchase-orders/{purchaseOrderId}/status`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Response Body (`PurchaseOrderStatusResponse`)
```json
{
  "status": "CHECKED_OUT",
  "customerOrder": {
    "customerOrderId": "com-order-uuid",
    "status": "PROCESSING",
    "steps": [
      {
        "step": "PAYMENT",
        "status": "IN_PROGRESS",
        "startedAt": "2026-03-30T10:00:00",
        "endedAt": null
      }
    ]
  }
}
```

Lança `NotFoundException` (404) se não encontrado.

---

## 4. Listar Pedidos por Cliente

**`GET /purchase-orders`** → `200 OK`

### Query Parameters
| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `customerId` | String | **sim** | ID do cliente |
| `status` | String | não | Filtro por status (OPENED, CHECKED_OUT, COMPLETED, FAILED, CANCELED, DELETED) |
| `start` | String | não | Data de início (formato ISO 8601) |
| `end` | String | não | Data de fim (formato ISO 8601) |

### Response Body
`List<PurchaseOrderResponse>` — lista vazia `[]` se nenhum resultado.

---

## Status de Pedido — Valores Possíveis

| Status | Descrição |
|--------|-----------|
| `OPENED` | Pedido criado, ainda em montagem |
| `CHECKED_OUT` | Checkout realizado, aguardando processamento no COM |
| `COMPLETED` | Pedido concluído com sucesso |
| `FAILED` | Pedido falhou durante o processamento |
| `CANCELED` | Pedido cancelado |
| `DELETED` | Pedido deletado |
