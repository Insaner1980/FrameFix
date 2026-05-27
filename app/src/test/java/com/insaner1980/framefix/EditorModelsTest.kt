package com.insaner1980.framefix

import com.insaner1980.framefix.model.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorModelsTest {

    @Test
    fun `preset ratio values match labels`() {
        assertEquals(9f / 16f, AspectRatio.Ratio9_16.ratioValue, 0.001f)
        assertEquals(4f / 5f, AspectRatio.Ratio4_5.ratioValue, 0.001f)
        assertEquals(3f / 4f, AspectRatio.Ratio3_4.ratioValue, 0.001f)
        assertEquals(2f / 3f, AspectRatio.Ratio2_3.ratioValue, 0.001f)
        assertEquals(1f, AspectRatio.Ratio1_1.ratioValue, 0.001f)
        assertEquals(16f / 9f, AspectRatio.Ratio16_9.ratioValue, 0.001f)
        assertEquals(4f / 3f, AspectRatio.Ratio4_3.ratioValue, 0.001f)
        assertEquals(3f / 2f, AspectRatio.Ratio3_2.ratioValue, 0.001f)
    }

    @Test
    fun `custom ratio value follows changed width and height`() {
        val ratio = AspectRatio.Custom(3f, 2f)

        ratio.w = 1f
        ratio.h = 1f

        assertEquals(1f, ratio.ratioValue, 0.001f)
    }

    @Test
    fun `custom ratio falls back to square when height is not positive`() {
        val ratio = AspectRatio.Custom(4f, 0f)

        assertEquals(1f, ratio.ratioValue, 0.001f)
    }
}
