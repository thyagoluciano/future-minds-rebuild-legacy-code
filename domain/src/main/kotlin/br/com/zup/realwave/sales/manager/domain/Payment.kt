package br.com.zup.realwave.sales.manager.domain

data class Payment(
    val methods: MutableList<PaymentMethod> = mutableListOf(),
    val description: String? = null
) {
    companion object {
        fun couponPayment(): Payment = Payment(
            methods = mutableListOf(PaymentMethod(type = "REWARD")),
            description = null
        )
    }
}
