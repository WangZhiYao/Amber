package cn.floriax.amber.domain.device.usecase

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.DiscoveredDevice
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.device.repository.DeviceRepository
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 启动自动连用例单测：有默认且蓝牙开 → 直连；无默认/蓝牙关 → 静默跳过。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class AutoConnectUseCaseTest {

    private val fakeDevices = FakeDevices()

    private var bluetoothEnabled = true

    private val fakeScanner = object : DeviceScanner {
        override val isBluetoothEnabled get() = bluetoothEnabled
        override fun scan(): Flow<List<DiscoveredDevice>> = flowOf(emptyList())
    }

    private val fakeClock = FakeClock()

    private val useCase = AutoConnectUseCase(fakeDevices, fakeScanner, fakeClock)

    @Test
    fun `有默认设备且蓝牙开时自动连接`() = runTest {
        fakeDevices.default = ClockDevice("EC:B2", "钟", "NIXIE", true, 0)

        useCase()

        assertEquals("EC:B2", fakeClock.connectedMac)
    }

    @Test
    fun `无默认设备时静默跳过`() = runTest {
        useCase()

        assertNull(fakeClock.connectedMac)
    }

    @Test
    fun `蓝牙关闭时不尝试连接`() = runTest {
        fakeDevices.default = ClockDevice("EC:B2", "钟", "NIXIE", true, 0)
        bluetoothEnabled = false

        useCase()

        assertNull(fakeClock.connectedMac)
    }

    private class FakeDevices : DeviceRepository {
        val devices = MutableStateFlow<List<ClockDevice>>(emptyList())
        var default: ClockDevice? = null
        override fun observeDevices(): Flow<List<ClockDevice>> = devices
        override suspend fun defaultDevice(): ClockDevice? = default
        override suspend fun upsert(device: ClockDevice) = Unit
        override suspend fun setDefault(mac: String) = Unit
        override suspend fun setDefaultIfNone(mac: String) = Unit
        override suspend fun rename(mac: String, alias: String) = Unit
        override suspend fun delete(mac: String) = Unit
    }

    private class FakeClock : ClockRepository {
        override val deviceState = MutableStateFlow(DeviceState())
        override val frameLogs = MutableStateFlow<List<FrameLog>>(emptyList())
        var connectedMac: String? = null
        override suspend fun connect(device: ClockDevice): Result<Unit> {
            connectedMac = device.mac
            return Result.success(Unit)
        }

        override fun disconnect() = Unit
        override suspend fun sendBacklight(config: Backlight) = Result.success(Unit)
        override suspend fun sendTimers(timers: TimeConfig) = Result.success(Unit)
        override suspend fun sendSwitches(config: SwitchConfig) = Result.success(Unit)
        override suspend fun syncTime() = Result.success(Unit)
        override suspend fun sendRaw(bytes: ByteArray) = Result.success(Unit)
    }
}
