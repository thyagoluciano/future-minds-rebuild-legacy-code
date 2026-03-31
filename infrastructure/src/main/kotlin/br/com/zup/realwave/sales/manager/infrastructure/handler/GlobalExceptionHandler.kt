package br.com.zup.realwave.sales.manager.infrastructure.handler

import br.com.zup.realwave.sales.manager.domain.exception.InvalidStatusTransitionException
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderNotFoundException
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderValidationException
import feign.FeignException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorDetail(val code: String, val message: String)

data class ErrorResponse(val errors: List<ErrorDetail>)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PurchaseOrderNotFoundException::class)
    fun handleNotFound(ex: PurchaseOrderNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "PURCHASE_ORDER_NOT_FOUND", message = ex.message ?: "PurchaseOrder not found"))
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(PurchaseOrderValidationException::class)
    fun handleValidation(ex: PurchaseOrderValidationException): ResponseEntity<ErrorResponse> {
        val errors = ex.errors.map { ErrorDetail(code = "PURCHASE_ORDER_VALIDATION_ERROR", message = it) }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse(errors = errors))
    }

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidStatusTransition(ex: InvalidStatusTransitionException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "INVALID_STATUS_TRANSITION", message = ex.message ?: "Invalid status transition"))
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { fieldError ->
            ErrorDetail(
                code = "VALIDATION_ERROR",
                message = "${fieldError.field}: ${fieldError.defaultMessage}"
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(errors = errors))
    }

    @ExceptionHandler(FeignException.NotFound::class)
    fun handleFeignNotFound(ex: FeignException.NotFound): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_NOT_FOUND", message = ex.message ?: "External service resource not found"))
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(FeignException.BadRequest::class)
    fun handleFeignBadRequest(ex: FeignException.BadRequest): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_BAD_REQUEST", message = ex.message ?: "Bad request to external service"))
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(FeignException.UnprocessableEntity::class)
    fun handleFeignUnprocessableEntity(ex: FeignException.UnprocessableEntity): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_UNPROCESSABLE_ENTITY", message = ex.message ?: "Unprocessable entity from external service"))
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response)
    }

    @ExceptionHandler(FeignException.InternalServerError::class)
    fun handleFeignInternalServerError(ex: FeignException.InternalServerError): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_ERROR", message = ex.message ?: "External service internal error"))
        )
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response)
    }

    @ExceptionHandler(feign.RetryableException::class)
    fun handleFeignTimeout(ex: feign.RetryableException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_TIMEOUT", message = ex.message ?: "External service timeout"))
        )
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response)
    }

    @ExceptionHandler(FeignException::class)
    fun handleFeignException(ex: FeignException): ResponseEntity<ErrorResponse> {
        val status = if (ex.status() in 400..499) HttpStatus.valueOf(ex.status()) else HttpStatus.BAD_GATEWAY
        val response = ErrorResponse(
            errors = listOf(ErrorDetail(code = "EXTERNAL_SERVICE_ERROR", message = ex.message ?: "External service error"))
        )
        return ResponseEntity.status(status).body(response)
    }
}
