package br.com.zup.realwave.sales.manager.api.request.validation

import br.com.zup.realwave.sales.manager.api.request.PaymentRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [(PaymentMethodValidator::class)])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PaymentMethodValidation(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class PaymentMethodValidator : ConstraintValidator<PaymentMethodValidation, Any> {
    companion object {
        private const val BOLETO = "BOLETO"
        private const val METHOD_ID = "methodId"
        private const val NOT_NULL_NOR_BLANK_MSG = "may not be null nor blank"
    }

    private var message: String? = null
    private var groups: Array<KClass<*>> = arrayOf()
    private var payload: Array<KClass<out Payload>> = arrayOf()

    override fun initialize(annotation: PaymentMethodValidation) {
        message = annotation.message
        groups = annotation.groups
        payload = annotation.payload
    }

    override fun isValid(objectToValidate: Any, context: ConstraintValidatorContext): Boolean {
        val paymentMethodRequest = objectToValidate as PaymentRequest.PaymentMethodRequest

        if (paymentMethodRequest.method?.uppercase() != BOLETO && paymentMethodRequest.methodId.isNullOrBlank()) {
            constraintViolation(METHOD_ID, context, NOT_NULL_NOR_BLANK_MSG)
            return false
        }

        return true
    }

    private fun constraintViolation(fieldValue: String, context: ConstraintValidatorContext, message: String) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(fieldValue)
            .addConstraintViolation()
    }
}
