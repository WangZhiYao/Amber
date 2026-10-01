package cn.floriax.amber.domain.device

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.DiscoveredDevice
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 设备管理协调器单测：扫描收集/去重、连接落库设默认、删除联动断开。
 * 弹窗三页共用同一单例，这里的规则就是弹窗的全部行为契约。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceManagerCoordinatorTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val fakeDevices = FakeDeviceRepository()

    /** Scan snapshots: the scanner contract emits the accumulated list. */
    private val scanSnapshots = MutableSharedFlow<List<DiscoveredDevice>>(extraBufferCapacity = 16)

    /** Mutable scanner fake: toggling Bluetooth / failing scan included. */
    private inner class MutableScanner : DeviceScanner {
        var enabled = true
        var failScan: RuntimeException? = null
        override val isBluetoothEnabled get() = enabled
        override fun scan(): Flow<List<DiscoveredDevice>> =
            failScan?.let { e -> kotlinx.coroutines.flow.flow { throw e } } ?: scanSnapshots
    }

    private val fakeScanner = MutableScanner()

    private val fakeClock = FakeClockRepository()

    private lateinit var coordinator: DeviceManagerCoordinator

    private fun setUpCoordinator(scope: CoroutineScope = testScope.backgroundScope) {
        coordinator = DeviceManagerCoordinator(
            devices = fakeDevices,
            scanner = fakeScanner,
            clock = fakeClock,
            scope = scope,
        )
    }

    @Test
    fun `扫描结果镜像扫描器的累积快照`() {
        setUpCoordinator()
        coordinator.startScan()

        scanSnapshots.tryEmit(listOf(DiscoveredDevice("NIXIE", "EC:B2:A2:02:99:1F", -66)))
        scanSnapshots.tryEmit(
            listOf(
                DiscoveredDevice("NIXIE", "EC:B2:A2:02:99:1F", -58),
                DiscoveredDevice("NIXIE", "AA:BB:CC:DD:EE:FF", -70),
            ),
        )

        val results = coordinator.state.value.scanResults
        assertEquals(2, results.size)
        assertEquals(-58, results.first { it.mac == "EC:B2:A2:02:99:1F" }.rssi)
    }

    @Test
    fun `已保存的设备不出现在扫描结果`() {
        fakeDevices.devices.value = listOf(
            ClockDevice("EC:B2:A2:02:99:1F", "客厅的钟", "NIXIE", true, 0L),
        )
        setUpCoordinator()
        coordinator.startScan()

        scanSnapshots.tryEmit(
            listOf(
                DiscoveredDevice("NIXIE", "EC:B2:A2:02:99:1F", -58),
                DiscoveredDevice("NIXIE", "AA:BB:CC:DD:EE:FF", -70),
            ),
        )

        // The saved device has its connect entry in "my devices"; the scan
        // section lists new devices only.
        assertEquals(listOf("AA:BB:CC:DD:EE:FF"), coordinator.state.value.scanResults.map { it.mac })
    }

    @Test
    fun `蓝牙关闭时不启动扫描并置失败状态`() {
        setUpCoordinator()
        fakeScanner.enabled = false

        coordinator.startScan()

        val state = coordinator.state.value
        assertFalse(state.scanning)
        assertTrue(state.scanFailed)
    }

    @Test
    fun `扫描流异常不向上传播并置失败状态`() {
        setUpCoordinator()
        fakeScanner.failScan = RuntimeException("BLUETOOTH_DISABLED")

        // Unconfined scope: an uncaught exception here would fail the
        // test itself — reaching the asserts proves it was contained.
        coordinator.startScan()

        val state = coordinator.state.value
        assertFalse(state.scanning)
        assertTrue(state.scanFailed)
    }

    @Test
    fun `扫描结束或停止后扫描状态复位`() {
        setUpCoordinator()
        coordinator.startScan()
        assertTrue(coordinator.state.value.scanning)

        coordinator.stopScan()

        val state = coordinator.state.value
        assertFalse(state.scanning)
        assertTrue(state.scanResults.isEmpty())
    }

    @Test
    fun `连接设备时落库并设为默认`() {
        setUpCoordinator()
        coordinator.startScan()

        coordinator.connectDevice(DiscoveredDevice("NIXIE", "EC:B2:A2:02:99:1F", -66))
        testScope.advanceUntilIdle()

        val saved = fakeDevices.upserted.single()
        assertEquals("EC:B2:A2:02:99:1F", saved.mac)
        assertEquals("EC:B2:A2:02:99:1F", fakeDevices.defaultIfNoneSetFor)
        assertEquals("EC:B2:A2:02:99:1F", fakeClock.connectedMac)
        assertFalse(coordinator.state.value.scanning)
    }

    @Test
    fun `删除当前连接设备时联动断开`() {
        setUpCoordinator()
        fakeClock.deviceState.value = fakeClock.deviceState.value.copy(
            connection = ConnectionState.CONNECTED,
            deviceMac = "EC:B2:A2:02:99:1F",
        )

        coordinator.delete("EC:B2:A2:02:99:1F")

        assertEquals("EC:B2:A2:02:99:1F", fakeDevices.deletedMac)
        assertTrue(fakeClock.disconnected)
    }

    @Test
    fun `删除非连接设备不触发断开`() {
        setUpCoordinator()
        fakeClock.deviceState.value = fakeClock.deviceState.value.copy(
            connection = ConnectionState.CONNECTED,
            deviceMac = "EC:B2:A2:02:99:1F",
        )

        coordinator.delete("AA:BB:CC:DD:EE:FF")

        assertEquals("AA:BB:CC:DD:EE:FF", fakeDevices.deletedMac)
        assertFalse(fakeClock.disconnected)
    }

    @Test
    fun `设备列表流反映到状态`() {
        fakeDevices.devices.value = listOf(
            ClockDevice("EC:B2:A2:02:99:1F", "客厅的钟", "NIXIE", true, 0L),
        )
        setUpCoordinator()

        assertEquals("客厅的钟", coordinator.state.value.devices.single().alias)
    }

    @Test
    fun `当前连接mac反映到状态`() {
        setUpCoordinator()
        fakeClock.deviceState.value = fakeClock.deviceState.value.copy(
            connection = ConnectionState.CONNECTED,
            deviceMac = "EC:B2:A2:02:99:1F",
        )

        assertEquals("EC:B2:A2:02:99:1F", coordinator.state.value.connectedMac)
    }

    @Test
    fun `从列表连接已存设备`() {
        setUpCoordinator()
        val saved = ClockDevice("EC:B2:A2:02:99:1F", "客厅的钟", "NIXIE", true, 0L)

        coordinator.connectSaved(saved)
        testScope.advanceUntilIdle()

        // 连接前刷新落库（更新 lastConnectedAt），再发起连接。
        assertEquals("EC:B2:A2:02:99:1F", fakeDevices.upserted.single().mac)
        assertTrue(fakeDevices.upserted.single().lastConnectedAt > 0)
        assertEquals("EC:B2:A2:02:99:1F", fakeClock.connectedMac)
    }

    @Test
    fun `断开当前连接`() {
        setUpCoordinator()

        coordinator.disconnect()
        testScope.advanceUntilIdle()

        assertTrue(fakeClock.disconnected)
    }
}

/** In-memory DeviceRepository，记录写操作。 */
private class FakeDeviceRepository : cn.floriax.amber.domain.device.repository.DeviceRepository {
    val devices = MutableStateFlow<List<ClockDevice>>(emptyList())
    override fun observeDevices(): Flow<List<ClockDevice>> = devices
    val upserted = mutableListOf<ClockDevice>()
    var defaultDevice: ClockDevice? = null
    var defaultSetFor: String? = null
    var defaultIfNoneSetFor: String? = null
    var renamedTo: Pair<String, String>? = null
    var deletedMac: String? = null

    override suspend fun defaultDevice(): ClockDevice? = defaultDevice

    override suspend fun upsert(device: ClockDevice) {
        upserted.add(device)
    }

    override suspend fun setDefault(mac: String) {
        defaultSetFor = mac
    }

    override suspend fun setDefaultIfNone(mac: String) {
        defaultIfNoneSetFor = mac
    }

    override suspend fun rename(mac: String, alias: String) {
        renamedTo = mac to alias
    }

    override suspend fun delete(mac: String) {
        deletedMac = mac
    }
}

/** 最小 ClockRepository fake：只关心连接/断开/设备状态。 */
private class FakeClockRepository : cn.floriax.amber.domain.device.ClockRepository {
    override val deviceState = MutableStateFlow(DeviceState())
    override val frameLogs = MutableStateFlow<List<FrameLog>>(emptyList())
    var connectedMac: String? = null
    var disconnected = false

    override suspend fun connect(device: ClockDevice): Result<Unit> {
        connectedMac = device.mac
        return Result.success(Unit)
    }

    override fun disconnect() {
        disconnected = true
    }

    override suspend fun sendBacklight(config: Backlight) = Result.success(Unit)
    override suspend fun sendTimers(timers: TimeConfig) = Result.success(Unit)
    override suspend fun sendSwitches(config: SwitchConfig) = Result.success(Unit)
    override suspend fun syncTime() = Result.success(Unit)
    override suspend fun sendRaw(bytes: ByteArray) = Result.success(Unit)
}
