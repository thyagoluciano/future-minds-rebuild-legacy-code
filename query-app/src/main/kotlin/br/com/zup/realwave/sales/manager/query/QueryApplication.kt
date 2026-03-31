package br.com.zup.realwave.sales.manager.query

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
class QueryApplication

fun main(args: Array<String>) {
    runApplication<QueryApplication>(*args)
}
