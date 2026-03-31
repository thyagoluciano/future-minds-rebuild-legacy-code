package br.com.zup.realwave.sales.manager.consumer.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
