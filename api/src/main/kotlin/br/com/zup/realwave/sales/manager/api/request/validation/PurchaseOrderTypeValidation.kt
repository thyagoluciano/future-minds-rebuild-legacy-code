package br.com.zup.realwave.sales.manager.api.request.validation

import br.com.zup.realwave.sales.manager.api.request.PurchaseOrderRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [(PurchaseOrderTypeValidator::class)])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PurchaseOrderTypeValidation(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class PurchaseOrderTypeValidator : ConstraintValidator<PurchaseOrderTypeValidation, Any> {
    private var message: String? = null
    private var groups: Array<KClass<*>> = arrayOf()
    private var payload: Array<KClass<out Payload>> = arrayOf()

    override fun initialize(annotation: PurchaseOrderTypeValidation) {
        message = annotation.message
        groups = annotation.groups
        payload = annotation.payload
    }

    override fun isValid(objectToValidate: Any, context: ConstraintValidatorContext): Boolean {
        val purchaseOrderRequest = objectToValidate as PurchaseOrderRequest
        return purchaseOrderRequest.type
            ?.isValidPurchaseOrderType()
            ?.also { isValid ->
                if (!isValid) this.constraintViolation("type", context, "Invalid Type")
            } ?: true
    }

    fun constraintViolation(fieldValue: String, context: ConstraintValidatorContext, message: String) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(fieldValue)
            .addConstraintViolation()
    }

    private fun String.isValidPurchaseOrderType() =
        PurchaseOrderType.values().any { it.name == this }

    enum class PurchaseOrderType {
        JOIN,
        CHANGE,
        BUY,
        COUPON
    }
}
