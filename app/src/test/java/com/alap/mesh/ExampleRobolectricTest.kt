package com.alap.mesh

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.alap.mesh.data.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("আলাপ", appName)
    }

    @Test
    fun `initial settings have zero fake counters and valid generated node id`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = DataStoreManager(context)
        val nodeId = manager.ensureNodeIdInitialized()
        val settings = manager.settingsFlow.first()

        assertTrue(nodeId.matches(Regex("^ALAP-[0-9A-F]{4}-[0-9A-F]{3}$")))
        assertEquals(nodeId, settings.nodeId)
        assertEquals(0, settings.relayedMessageCount)
        assertEquals(0, settings.sharedFilesCount)
        assertEquals(0, settings.txPacketCount)
        assertEquals(0, settings.rxPacketCount)
    }
}
