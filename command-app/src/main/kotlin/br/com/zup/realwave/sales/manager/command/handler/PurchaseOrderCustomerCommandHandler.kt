package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.command.UpdateCustomerCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderCustomerCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: UpdateCustomerCommand) {
        log.debug("Handling UpdateCustomerCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateCustomer(command)
        repository.save(order)
        eventPublisher.publish(order)
    }
}
