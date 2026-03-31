package br.com.zup.realwave.sales.manager.command.handler

import br.com.zup.realwave.sales.manager.domain.command.DeleteMgmCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateMgmCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PurchaseOrderMgmCommandHandler(
    private val repository: PurchaseOrderRepository,
    private val eventPublisher: PurchaseOrderEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(command: UpdateMgmCommand) {
        log.debug("Handling UpdateMgmCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.updateMgm(command)
        repository.save(order)
        eventPublisher.publish(order)
    }

    fun handle(command: DeleteMgmCommand) {
        log.debug("Handling DeleteMgmCommand purchaseOrderId={}", command.id)
        val order = repository.findByIdOrThrow(command.id)
        order.deleteMgm(command)
        repository.save(order)
        eventPublisher.publish(order)
    }
}
