package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.command.RemoveSalesForceCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateSalesForceCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderSalesForceCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: UpdateSalesForceCommand) {
        log.debug("Handling UpdateSalesForceCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateSalesForce(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: RemoveSalesForceCommand) {
        log.debug("Handling RemoveSalesForceCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.removeSalesForce(command)
        repository.save(order)
        eventPublisher.publish(order)
    }
}
