package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SegmentationTest {

    @Test
    fun `should create Segmentation with custom fields`() {
        val fields = mapOf("key" to "value", "age" to 30)
        val segmentation = Segmentation(customFields = fields)
        assertEquals(fields, segmentation.customFields)
    }

    @Test
    fun `should create Segmentation with empty custom fields`() {
        val segmentation = Segmentation(customFields = emptyMap())
        assertTrue(segmentation.customFields.isEmpty())
    }

    @Test
    fun `two Segmentations with same data should be equal`() {
        val s1 = Segmentation(mapOf("k" to "v"))
        val s2 = Segmentation(mapOf("k" to "v"))
        assertEquals(s1, s2)
    }
}
