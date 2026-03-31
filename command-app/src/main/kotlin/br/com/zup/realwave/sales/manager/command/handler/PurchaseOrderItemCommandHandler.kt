package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.command.AddItemCommand
import br.com.zup.realwave.sales.manager.domain.command.RemoveItemCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateItemCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderItemCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: AddItemCommand) {
        log.debug("Handling AddItemCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.addItem(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: UpdateItemCommand) {
        log.debug("Handling UpdateItemCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateItem(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: RemoveItemCommand) {
        log.debug("Handling RemoveItemCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.removeItem(command)
        repository.save(order)
        eventPublisher.publish(order)
    }
}
