package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.command.DeleteInstallationAttributesCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateInstallationAttributesCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderInstallationAttributesCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: UpdateInstallationAttributesCommand) {
        log.debug("Handling UpdateInstallationAttributesCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateInstallationAttributes(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: DeleteInstallationAttributesCommand) {
        log.debug("Handling DeleteInstallationAttributesCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.deleteInstallationAttributes(command)
        repository.save(order)
        eventPublisher.publish(order)
    }
}
