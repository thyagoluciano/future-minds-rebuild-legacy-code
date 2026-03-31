package br.com.zup.realwave.sales.manager.domain.exception

class InvalidStatusTransitionException(from: String, to: String) :
    RuntimeException("Invalid status transition from '$from' to '$to'")
