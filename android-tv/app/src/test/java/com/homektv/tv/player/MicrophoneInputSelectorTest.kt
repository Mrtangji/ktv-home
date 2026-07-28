package com.homektv.tv.player

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophoneInputSelectorTest {
    @Test
    fun `prefers usb then wired then bluetooth inputs`() {
        assertTrue(
            MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_USB_HEADSET) <
                MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_WIRED_HEADSET),
        )
        assertTrue(
            MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_WIRED_HEADSET) <
                MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_BLE_HEADSET),
        )
        assertTrue(
            MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_BLE_HEADSET) <
                MicrophoneInputSelector.priority(AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
        )
    }

    @Test
    fun `labels supported microphone types`() {
        assertEquals("USB 麦克风", MicrophoneInputSelector.label(AudioDeviceInfo.TYPE_USB_DEVICE))
        assertEquals("有线麦克风", MicrophoneInputSelector.label(AudioDeviceInfo.TYPE_WIRED_HEADSET))
        assertEquals("蓝牙麦克风", MicrophoneInputSelector.label(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
    }

    @Test
    fun `allows vendor default routed input only for verified tv model`() {
        assertTrue(MicrophoneInputSelector.allowsDefaultRoutedInput("Q601F"))
        assertTrue(MicrophoneInputSelector.allowsDefaultRoutedInput("q601f"))
        assertEquals(false, MicrophoneInputSelector.allowsDefaultRoutedInput("generic-tv"))
    }
}
