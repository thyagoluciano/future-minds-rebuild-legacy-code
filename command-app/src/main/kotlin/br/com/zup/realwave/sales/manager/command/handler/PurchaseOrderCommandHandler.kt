package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCouponCommand
import br.com.zup.realwave.sales.manager.domain.command.DeletePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.FindPurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdatePurchaseOrderTypeCommand
import br.com.zup.realwave.sales.manager.domain.command.ValidatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderCheckoutService
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher,
    private val validator: PurchaseOrderValidator,
    private val checkoutService: PurchaseOrderCheckoutService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: CreatePurchaseOrderCommand): PurchaseOrderId {
        log.debug("Handling CreatePurchaseOrderCommand id={}", command.id)
        val order = PurchaseOrder.create(command)
        repository.save(order)
        eventPublisher.publish(order)
        return order.id
    }

    fun handle(command: CreatePurchaseOrderCouponCommand): PurchaseOrderId {
        log.debug("Handling CreatePurchaseOrderCouponCommand id={}", command.id)
        val order = PurchaseOrder.createWithCoupon(command)
        repository.save(order)
        eventPublisher.publish(order)
        return order.id
    }

    fun handle(command: DeletePurchaseOrderCommand) {
        log.debug("Handling DeletePurchaseOrderCommand id={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.delete(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: UpdatePurchaseOrderTypeCommand): PurchaseOrderId {
        log.debug("Handling UpdatePurchaseOrderTypeCommand id={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateType(command)
        repository.save(order)
        eventPublisher.publish(order)
        return order.id
    }

    fun handle(command: ValidatePurchaseOrderCommand) {
        log.debug("Handling ValidatePurchaseOrderCommand id={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        validator.validate(order)
    }

    fun handle(command: FindPurchaseOrderCommand): PurchaseOrder {
        log.debug("Handling FindPurchaseOrderCommand id={}", command.id)
        return repository.findByIdOrThrow(command.id)
    }

    fun handle(command: CheckoutCommand): CustomerOrder {
        log.debug("Handling CheckoutCommand id={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        validator.validate(order)
        val customerOrder = checkoutService.checkout(order, command)
        order.checkout(command, customerOrder)
        repository.save(order)
        eventPublisher.publish(order)
        return customerOrder
    }
}
