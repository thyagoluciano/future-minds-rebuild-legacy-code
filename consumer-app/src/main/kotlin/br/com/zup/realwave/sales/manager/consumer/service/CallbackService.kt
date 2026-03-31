package br.com.zup.realwave.sales.manager.consumer.service

import br.com.zup.realwave.sales.manager.api.response.CallbackResponse
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class CallbackService(private val restTemplate: RestTemplate) {

    private val logger = LoggerFactory.getLogger(CallbackService::class.java)

    fun notify(callback: CallbackResponse, payload: Any) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        callback.headers?.let { headersNode ->
            if (headersNode.isObject) {
                headersNode.fields().forEach { (k, v) -> headers.set(k, v.asText()) }
            }
        }
        val entity = HttpEntity(payload, headers)
        try {
            restTemplate.postForEntity(callback.url, entity, String::class.java)
            logger.info("Callback notified successfully to: ${callback.url}")
        } catch (ex: Exception) {
            logger.error("Failed to notify callback at ${callback.url}: ${ex.message}", ex)
        }
    }
}
