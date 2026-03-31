---
paths:
  - "sales-manager-domain/**"
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