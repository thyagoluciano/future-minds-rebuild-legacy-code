package br.com.zup.realwave.sales.manager.command

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableFeignClients(basePackages = ["br.com.zup.realwave.sales.manager.infrastructure.feign"])
@EnableScheduling
class CommandApplication

fun main(args: Array<String>) {
    runApplication<CommandApplication>(*args)
}
