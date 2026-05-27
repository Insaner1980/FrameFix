package com.insaner1980.framefix

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FrameFix", appName)
    }

    @Test
    fun `uses FrameFix application id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.insaner1980.framefix", context.packageName)
    }
}
