package br.com.zup.realwave.sales.manager.api.request.validation

import br.com.zup.realwave.sales.manager.api.request.ItemRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [(ItemRequestValidator::class)])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ItemRequestValidation(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ItemRequestValidator : ConstraintValidator<ItemRequestValidation, Any> {
    private var message: String? = null
    private var groups: Array<KClass<*>> = arrayOf()
    private var payload: Array<KClass<out Payload>> = arrayOf()

    override fun initialize(annotation: ItemRequestValidation) {
        message = annotation.message
        groups = annotation.groups
        payload = annotation.payload
    }

    override fun isValid(objectToValidate: Any, context: ConstraintValidatorContext): Boolean {
        val itemRequest = objectToValidate as ItemRequest
        if (itemRequest.validity !== null && !itemRequest.validity.unlimited) return this.validator(
            itemRequest,
            context
        )
        return true
    }

    private fun validator(itemRequest: ItemRequest, context: ConstraintValidatorContext) =
        when {
            itemRequest.validity!!.duration == null -> buildConstraintMessage(context, "duration")
            itemRequest.validity.period == null -> buildConstraintMessage(context, "period")
            else -> true
        }

    private fun buildConstraintMessage(context: ConstraintValidatorContext, field: String): Boolean {
        this.constraintViolation("validity.$field", context, "it can't be null")
        return false
    }

    private fun constraintViolation(fieldValue: String, context: ConstraintValidatorContext, message: String) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(fieldValue)
            .addConstraintViolation()
    }
}
