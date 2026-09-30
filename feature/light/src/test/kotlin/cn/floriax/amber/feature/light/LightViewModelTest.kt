package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.device.usecase.ScanAndConnectUseCase
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 滑条「拖动预览 / 松手提交」契约单测：拖动过程（preview*）只更新 UI 状态
 * 不写设备——BLE 帧只在松手（set*）时发一次，否则拖动中每一步都发帧，
 * 设备响应不及时。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LightViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    /** Records every backlight config the ViewModel tried to write. */
    private val sentBacklights = mutableListOf<Backlight>()

    /** Releases the first in-flight sendBacklight call (simulates a slow device ack). */
    private val firstWriteGate = kotlinx.coroutines.CompletableDeferred<Unit>()

    /** When set, the first sendBacklight stays in flight until [firstWriteGate]. */
    private var holdFirstWrite = false

    private var writeCount = 0

    private val fakeRepository = object : ClockRepository {
        override val deviceState = MutableStateFlow(DeviceState())
        override val frameLogs = MutableStateFlow<List<FrameLog>>(emptyList())
        override suspend fun connect(device: ClockDevice) = Result.success(Unit)
        override fun disconnect() = Unit
        override suspend fun sendBacklight(config: Backlight): Result<Unit> {
            writeCount++
            // First write stays in flight until the test releases it — the
            // queuing behaviour under test only shows with a slow ack.
            if (writeCount == 1 && holdFirstWrite) firstWriteGate.await()
            sentBacklights.add(config)
            return Result.success(Unit)
        }
        override suspend fun sendTimers(timers: TimeConfig) = Result.success(Unit)
        override suspend fun sendSwitches(config: SwitchConfig) = Result.success(Unit)
        override suspend fun syncTime() = Result.success(Unit)
        override suspend fun sendRaw(bytes: ByteArray) = Result.success(Unit)
    }

    private val fakeScanner = object : DeviceScanner {
        override val isBluetoothEnabled = true
        override fun scan() = kotlinx.coroutines.flow.flowOf(emptyList<cn.floriax.amber.domain.device.model.DiscoveredDevice>())
    }

    private lateinit var viewModel: LightViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LightViewModel(fakeRepository, ScanAndConnectUseCase(fakeScanner, fakeRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `拖动中预览亮度不发帧`() {
        viewModel.previewBrightness(128)
        assertEquals(128, viewModel.uiState.value.backlight.brightness)
        assertEquals(0, sentBacklights.size)
    }

    @Test
    fun `松手提交亮度发一帧`() {
        viewModel.setBrightness(128)
        assertEquals(128, viewModel.uiState.value.backlight.brightness)
        assertEquals(1, sentBacklights.size)
        assertEquals(128, sentBacklights.single().brightness)
    }

    @Test
    fun `拖动中预览对比度不发帧`() {
        viewModel.previewSaturation(200)
        assertEquals(200, viewModel.uiState.value.backlight.saturations[0])
        assertEquals(0, sentBacklights.size)
    }

    @Test
    fun `松手提交对比度发一帧`() {
        viewModel.setSaturation(200)
        assertEquals(200, viewModel.uiState.value.backlight.saturations[0])
        assertEquals(1, sentBacklights.size)
        assertEquals(200, sentBacklights.single().saturations[0])
    }

    @Test
    fun `开启同色模式时饱和度也四组同步`() {
        // Group 4's contrast was edited separately in per-group mode; turning
        // same-color on must sync S across groups too — it used to sync only
        // hues, leaving [FF,FF,FF,7A] on the wire (the odd group rendered
        // visibly brighter/whiter).
        viewModel.setSameColor(false)
        viewModel.selectGroup(3)
        viewModel.setSaturation(122)

        viewModel.setSameColor(true)

        assertEquals(
            List(4) { 122 },
            viewModel.uiState.value.backlight.saturations,
        )
    }

    @Test
    fun `在途写期间的连续提交只发最终值`() {
        holdFirstWrite = true
        // First commit goes in flight and hangs on the device ack.
        viewModel.setBrightness(100)
        // While it is in flight two more commits land: only the newest may
        // be written after the first completes — the middle one (200) would
        // otherwise reach the device late and replay a stale value.
        viewModel.setBrightness(200)
        viewModel.setBrightness(250)

        // Unconfined main: the in-flight write resumes and drains the
        // backlog synchronously on complete().
        firstWriteGate.complete(Unit)

        assertEquals(listOf(100, 250), sentBacklights.map { it.brightness })
    }
}
