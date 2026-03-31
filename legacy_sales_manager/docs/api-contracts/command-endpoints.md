# API Contracts — Command Endpoints (Escrita)

> Porta: **8080**  
> Base path: `/purchase-orders`  
> Content-Type obrigatório: `application/json;charset=UTF-8`  
> Autenticação: Bearer Token (Keycloak) + headers de tenant (ver `security.md`)

---

## Tipos Comuns (reutilizados em múltiplos endpoints)

### Price
```json
{
  "currency": "BRL",   // String, obrigatório @NotBlank
  "amount": 9990,      // Int, obrigatório @NotNull — valor em centavos
  "scale": 2           // Int, obrigatório @NotNull — casas decimais (9990 / 10^2 = 99.90)
}
```

### CallbackRequest
```json
{
  "url": "https://system/callback",  // String, obrigatório
  "headers": { "X-Api-Key": "abc" }  // JsonNode, opcional
}
```

### OfferValidity
```json
{
  "period": "MONTHLY",   // String, opcional
  "duration": 12,        // Int, opcional
  "unlimited": false     // Boolean
}
```

### Address
```json
{
  "city": "São Paulo",     // String, obrigatório @NotBlank
  "state": "SP",           // String, obrigatório @NotBlank
  "country": "BR",         // String, obrigatório @NotBlank
  "district": "Centro",    // String, obrigatório @NotBlank
  "name": "Casa",          // String, obrigatório @NotBlank
  "street": "Rua X",       // String, obrigatório @NotBlank
  "zipCode": "01310-100",  // String, obrigatório @NotBlank
  "number": "100",         // String, obrigatório @NotBlank
  "complement": "Ap 1"     // String, opcional
}
```

---

## 1. Criar Pedido de Compra

**`POST /purchase-orders`** → `201 Created`

### Request Body (`PurchaseOrderRequest`)
```json
{
  "type": "NORMAL",                         // String, opcional — @PurchaseOrderTypeValidation
  "customer": "cust-uuid-123",              // String, opcional — ID do cliente
  "callback": {                             // CallbackRequest, opcional
    "url": "https://system/callback",
    "headers": {}
  }
}
```

### Response Body (`CreatePurchaseOrderResponse`)
```json
{
  "id": "po-uuid-123"   // String — ID gerado para o novo pedido
}
```

**Command:** `CreatePurchaseOrderCommand`

---

## 2. Criar Pedido com Cupom

**`POST /purchase-orders/coupon`** → `201 Created`

### Request Body (`PurchaseOrderCouponRequest`)
```json
{
  "couponCode": "PROMO10",          // String, obrigatório @NotBlank
  "productId": "prod-uuid-456",     // String, obrigatório @NotBlank
  "customerId": "cust-uuid-123",    // String, obrigatório @NotBlank
  "callback": {                     // CallbackRequest, opcional
    "url": "https://system/callback",
    "headers": {}
  }
}
```

### Response Body (`CreatePurchaseOrderResponse`)
```json
{
  "id": "po-uuid-123"
}
```

**Command:** `CreatePurchaseOrderCouponCommand`

---

## 3. Deletar Pedido

**`DELETE /purchase-orders/{purchaseOrderId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Response Body (`DeleteResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123"
}
```

**Command:** `DeletePurchaseOrderCommand`

---

## 4. Atualizar Tipo do Pedido

**`PUT /purchase-orders/{purchaseOrderId}/type`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`PurchaseOrderRequest`)
```json
{
  "type": "COUPON",              // String, obrigatório @PurchaseOrderTypeValidation
  "customer": "cust-uuid-123",   // String, opcional
  "callback": null               // CallbackRequest, opcional
}
```

### Response Body (`PurchaseOrderTypeResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "type": "COUPON"
}
```

**Command:** `UpdatePurchaseOrderType`

---

## 5. Validar Pedido

**`GET /purchase-orders/{purchaseOrderId}/validation`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Response Body (`ValidateResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123"
}
```

Lança `PurchaseOrderValidationException` (400) se validação falhar.

**Command:** `ValidatePurchaseOrder`

---

## 6. Buscar Pedido (via Command App)

**`GET /purchase-orders/{purchaseOrderId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Response Body
Ver estrutura completa `PurchaseOrderResponse` em `query-endpoints.md`.

**Command:** `FindPurchaseOrderCommand`

---

## 7. Adicionar Item

**`POST /purchase-orders/{purchaseOrderId}/items`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`ItemRequest`) — validado por `@ItemRequestValidation`
```json
{
  "catalogOfferId": "offer-uuid-456",    // String, obrigatório @NotNull
  "catalogOfferType": "OFFER",           // String, obrigatório @NotNull
  "price": {                             // Price, obrigatório @NotNull
    "currency": "BRL",
    "amount": 9990,
    "scale": 2
  },
  "validity": {                          // OfferValidity, obrigatório @NotNull
    "period": "MONTHLY",
    "duration": 12,
    "unlimited": false
  },
  "offerFields": {},                     // JsonNode, opcional
  "customFields": {},                    // JsonNode, opcional
  "offerItems": [                        // List<OfferItem>, obrigatório @NotNull
    {
      "productId": "prod-uuid-789",              // String, opcional
      "catalogOfferItemId": "item-uuid-111",     // String, obrigatório @NotBlank
      "price": { "currency": "BRL", "amount": 4995, "scale": 2 },
      "recurrent": false,                        // Boolean, opcional
      "customFields": {},                        // JsonNode, opcional
      "userParameters": { "key": "value" }       // Map<String,Any>, opcional
    }
  ],
  "pricesPerPeriod": [                   // List<PricePerPeriodRequest>, opcional
    {
      "totalPrice": { "currency": "BRL", "amount": 9990, "scale": 2 },
      "totalDiscountPrice": { "currency": "BRL", "amount": 0, "scale": 2 },
      "totalPriceWithDiscount": { "currency": "BRL", "amount": 9990, "scale": 2 },
      "startAt": 0,                      // Int, obrigatório @NotNull — mês de início
      "endAt": 11,                       // Int, obrigatório @NotNull — mês de fim
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
  "quantity": 1                          // Int, default 1
}
```

### Response Body (`PurchaseOrderItemResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "itemId": "item-uuid-gerado"
}
```

**Command:** `AddItemCommand`

---

## 8. Atualizar Item

**`PUT /purchase-orders/{purchaseOrderId}/items/{itemId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório
- `itemId`: String, obrigatório

### Request Body
Idêntico ao `ItemRequest` do endpoint 7.

### Response Body (`PurchaseOrderItemResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "itemId": "item-uuid-111"
}
```

**Command:** `UpdateItemCommand`

---

## 9. Remover Item

**`DELETE /purchase-orders/{purchaseOrderId}/items/{catalogOfferId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório
- `catalogOfferId`: String, obrigatório

### Response Body (`PurchaseOrderItemResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "itemId": "offer-uuid-456"    // catalogOfferId removido
}
```

**Command:** `RemoveItemCommand`

---

## 10. Atualizar Pagamento

**`PUT /purchase-orders/{purchaseOrderId}/payment`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`PaymentRequest`)
```json
{
  "methods": [                          // List<PaymentMethodRequest>, obrigatório @Valid
    {
      "method": "CREDIT_CARD",          // String, obrigatório @NotBlank
                                        // Valores: CREDIT_CARD, DEBIT_CARD, BOLETO, REWARD, etc.
      "methodId": "card-uuid-333",      // String, opcional — ID do cartão/conta
      "price": {                        // Price, opcional
        "currency": "BRL",
        "amount": 9990,
        "scale": 2
      },
      "customFields": {},               // JsonNode, opcional
      "installments": 3                 // Int, opcional — número de parcelas
    }
  ],
  "description": "Pagamento mensal",    // String, opcional
  "async": false                        // Boolean, default false
}
```

### Response Body (`UpdatePaymentResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123"
}
```

**Command:** `UpdatePaymentCommand`

---

## 11. Atualizar Frete

**`PUT /purchase-orders/{purchaseOrderId}/freight`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`FreightRequest`)
```json
{
  "type": "NORMAL",                     // String, obrigatório @NotBlank
  "price": {                            // Price, obrigatório @Valid
    "currency": "BRL",
    "amount": 1000,
    "scale": 2
  },
  "address": {                          // Address, obrigatório @Valid
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
  "deliveryTotalTime": 7                // Int, obrigatório @NotNull — dias para entrega
}
```

### Response Body (`UpdateFreightResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123"
}
```

**Command:** `UpdateFreightCommand`

---

## 12. Atualizar Cupom

**`PUT /purchase-orders/{purchaseOrderId}/coupon`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`CouponRequest`)
```json
{
  "code": "PROMO10",             // String, obrigatório @NotBlank
  "customFields": { "key": "v" } // JsonNode, opcional
}
```

### Response Body (`UpdateCouponResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "code": "PROMO10",
  "customFields": {}
}
```

**Command:** `UpdateCouponCommand`

---

## 13. Atualizar Cliente

**`PUT /purchase-orders/{purchaseOrderId}/customer`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`CustomerRequest`)
```json
{
  "customer": "cust-uuid-123"   // String, obrigatório @NotBlank @Size(max=255)
}
```

### Response Body (`UpdateCustomerIdResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "customer": "cust-uuid-123"
}
```

**Command:** `UpdateCustomerCommand`

---

## 14. Checkout

**`POST /purchase-orders/{purchaseOrderId}/checkout`** → `201 Created`

### Path Variables
- `purchaseOrderId`: String, obrigatório

### Request Body (`CheckoutRequest`) — opcional (pode ser omitido)
```json
{
  "paymentSecurityCodes": [       // List<SecurityCode>, opcional @Valid
    {
      "methodId": "card-uuid-333",    // String, obrigatório @NotBlank
      "securityCode": "123"           // String, obrigatório @NotBlank — CVV
    }
  ]
}
```

### Response Body (`CheckoutResponse`)
```json
{
  "id": "po-uuid-123",
  "customerOrder": {
    "id": "com-order-uuid",          // ID retornado pelo CustomerOrderManager
    "boleto": {                      // presente apenas quando método = BOLETO
      "methodId": "boleto-uuid",
      "payload": {}                  // JsonNode com dados do boleto
    }
  }
}
```

### Status Codes
- `201` — sucesso
- `400` — falha na validação do pedido antes do checkout (PurchaseOrderValidationException)
  - Retorna `CheckoutResponse` com `customerOrder: null`

**Command:** `CheckoutCommand`

---

## 15. Callback do Customer Order Manager

**`POST /purchase-orders/callback`** → `204 No Content`

Endpoint chamado pelo Customer Order Manager quando o status de um pedido muda.

### Request Body (`CustomerOrderCallbackRequest`)
```json
{
  "id": "com-order-uuid",        // String, obrigatório @NotBlank — ID no COM
  "externalId": "po-uuid-123",   // String, obrigatório @NotBlank — purchaseOrderId
  "status": "FINISHED",          // String, obrigatório @NotBlank
  "steps": [                     // List<Step>, opcional
    {
      "step": "PAYMENT",
      "status": "COMPLETED",
      "startedAt": "2026-03-30T10:00:00",
      "endedAt": "2026-03-30T10:01:00",
      "processed": 1,
      "total": 1
    }
  ],
  "reason": {                    // Reason, opcional — preenchido em caso de falha
    "code": "PAYMENT_FAILED",
    "description": "Cartão recusado"
  }
}
```

### Response Body
Sem corpo (204).

**Command:** `UpdateCustomerOrderCommand`

---

## 16. Atualizar Atributos de Instalação

**`PUT /purchase-orders/{purchaseOrderId}/installation-attributes`** → `200 OK`

### Request Body (`InstallationAttributesRequest`)
```json
{
  "productTypeId": "ptype-uuid-444",   // String, obrigatório @NotBlank
  "attributes": {                       // Map<String,Any>, obrigatório @NotEmpty
    "speed": "100mbps",
    "technology": "FIBER"
  }
}
```

### Response Body (`UpdateInstallationAttributesResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "productTypeId": "ptype-uuid-444",
  "attributes": { "speed": "100mbps", "technology": "FIBER" }
}
```

**Command:** `UpdateInstallationAttributesCommand`

---

## 17. Deletar Atributos de Instalação

**`DELETE /purchase-orders/{purchaseOrderId}/installation-attributes/{productTypeId}`** → `200 OK`

### Path Variables
- `purchaseOrderId`: String, obrigatório
- `productTypeId`: String, obrigatório

### Response Body (`DeleteInstallationAttributesResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "productTypeId": "ptype-uuid-444"
}
```

**Command:** `DeleteInstallationAttributesCommand`

---

## 18. Atualizar MGM (Member-Get-Member)

**`PUT /purchase-orders/{purchaseOrderId}/mgm`** → `200 OK`

### Request Body (`MgmRequest`)
```json
{
  "code": "MGM-CODE-XYZ",         // String, obrigatório @NotBlank
  "customFields": { "key": "v" }  // JsonNode, opcional
}
```

### Response Body (`PurchaseOrderMgmResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "code": "MGM-CODE-XYZ",
  "customFields": {}
}
```

**Command:** `UpdateMgmCommand`

---

## 19. Deletar MGM

**`DELETE /purchase-orders/{purchaseOrderId}/mgm`** → `200 OK`

### Response Body (`PurchaseOrderMgmResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "code": null,
  "customFields": null
}
```

**Command:** `DeleteMgmCommand`

---

## 20. Atualizar Segmentação

**`PUT /purchase-orders/{purchaseOrderId}/segmentation`** → `200 OK`

### Request Body
`JsonNode` — objeto JSON livre com dados de segmentação.

### Response Body (`SegmentationResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "segmentation": { "query": { "field": "value" } }
}
```

**Command:** `UpdateSegmentationCommand`

---

## 21. Atualizar OnBoarding Sale

**`PUT /purchase-orders/{purchaseOrderId}/onboarding-sale`** → `200 OK`

### Request Body (`OnBoardingSaleRequest`)
```json
{
  "id": "offer-uuid-onboarding",  // String, obrigatório @NotBlank
  "customFields": { "key": "v" }  // JsonNode, opcional
}
```

### Response Body (`UpdateOnBoardingSaleResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "id": "offer-uuid-onboarding",
  "customFields": {}
}
```

**Command:** `UpdateOnBoardingSaleCommand`

---

## 22. Atualizar SalesForce

**`PUT /purchase-orders/{purchaseOrderId}/sales-force`** → `200 OK`

### Request Body (`SalesForceRequest`)
```json
{
  "id": "sf-uuid-555",        // String, obrigatório @NotBlank
  "name": "João Vendedor"     // String, obrigatório @NotBlank
}
```

### Response Body (`PurchaseOrderSalesForceResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "salesForceId": "sf-uuid-555"
}
```

**Command:** `UpdateSalesForceCommand`

---

## 23. Remover SalesForce

**`DELETE /purchase-orders/{purchaseOrderId}/sales-force`** → `200 OK`

### Response Body (`PurchaseOrderSalesForceResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "salesForceId": null
}
```

**Command:** `RemoveSalesForceCommand`

---

## 24. Atualizar Protocolo

**`PUT /purchase-orders/{purchaseOrderId}/protocol`** → `200 OK`

### Request Body (`ProtocolRequest`)
```json
{
  "protocol": "PROTO-2026-001"   // String, obrigatório @NotBlank
}
```

### Response Body (`ProtocolResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "protocol": "PROTO-2026-001"
}
```

**Command:** `UpdateProtocolCommand`

---

## 25. Atualizar Subscription

**`PUT /purchase-orders/{purchaseOrderId}/subscription`** → `200 OK`

### Request Body (`SubscriptionRequest`)
```json
{
  "id": "sub-uuid-666"   // String, obrigatório @NotBlank
}
```

### Response Body (`SubscriptionResponse`)
```json
{
  "purchaseOrderId": "po-uuid-123",
  "id": "sub-uuid-666"
}
```

**Command:** `UpdateSubscriptionCommand`

---

## Resumo dos Endpoints

| # | Método | Path | Status | Command |
|---|--------|------|--------|---------|
| 1 | POST | `/purchase-orders` | 201 | CreatePurchaseOrderCommand |
| 2 | POST | `/purchase-orders/coupon` | 201 | CreatePurchaseOrderCouponCommand |
| 3 | DELETE | `/purchase-orders/{id}` | 200 | DeletePurchaseOrderCommand |
| 4 | PUT | `/purchase-orders/{id}/type` | 200 | UpdatePurchaseOrderType |
| 5 | GET | `/purchase-orders/{id}/validation` | 200 | ValidatePurchaseOrder |
| 6 | GET | `/purchase-orders/{id}` | 200 | FindPurchaseOrderCommand |
| 7 | POST | `/purchase-orders/{id}/items` | 200 | AddItemCommand |
| 8 | PUT | `/purchase-orders/{id}/items/{itemId}` | 200 | UpdateItemCommand |
| 9 | DELETE | `/purchase-orders/{id}/items/{catalogOfferId}` | 200 | RemoveItemCommand |
| 10 | PUT | `/purchase-orders/{id}/payment` | 200 | UpdatePaymentCommand |
| 11 | PUT | `/purchase-orders/{id}/freight` | 200 | UpdateFreightCommand |
| 12 | PUT | `/purchase-orders/{id}/coupon` | 200 | UpdateCouponCommand |
| 13 | PUT | `/purchase-orders/{id}/customer` | 200 | UpdateCustomerCommand |
| 14 | POST | `/purchase-orders/{id}/checkout` | 201 | CheckoutCommand |
| 15 | POST | `/purchase-orders/callback` | 204 | UpdateCustomerOrderCommand |
| 16 | PUT | `/purchase-orders/{id}/installation-attributes` | 200 | UpdateInstallationAttributesCommand |
| 17 | DELETE | `/purchase-orders/{id}/installation-attributes/{ptid}` | 200 | DeleteInstallationAttributesCommand |
| 18 | PUT | `/purchase-orders/{id}/mgm` | 200 | UpdateMgmCommand |
| 19 | DELETE | `/purchase-orders/{id}/mgm` | 200 | DeleteMgmCommand |
| 20 | PUT | `/purchase-orders/{id}/segmentation` | 200 | UpdateSegmentationCommand |
| 21 | PUT | `/purchase-orders/{id}/onboarding-sale` | 200 | UpdateOnBoardingSaleCommand |
| 22 | PUT | `/purchase-orders/{id}/sales-force` | 200 | UpdateSalesForceCommand |
| 23 | DELETE | `/purchase-orders/{id}/sales-force` | 200 | RemoveSalesForceCommand |
| 24 | PUT | `/purchase-orders/{id}/protocol` | 200 | UpdateProtocolCommand |
| 25 | PUT | `/purchase-orders/{id}/subscription` | 200 | UpdateSubscriptionCommand |
