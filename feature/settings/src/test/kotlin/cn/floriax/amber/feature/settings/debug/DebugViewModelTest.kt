package cn.floriax.amber.feature.settings.debug

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.HexFormat

/**
 * 调试面板 ViewModel 单测：帧日志投影、输入实时校验、发送走仓库。
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val logs = MutableStateFlow<List<FrameLog>>(emptyList())

    private val sentRaw = mutableListOf<ByteArray>()

    private var sendResult: Result<Unit> = Result.success(Unit)

    private val fakeClock = object : ClockRepository {
        override val deviceState = MutableStateFlow(DeviceState())
        override val frameLogs = logs
        override suspend fun connect(device: ClockDevice) = Result.success(Unit)
        override fun disconnect() = Unit
        override suspend fun sendBacklight(config: Backlight) = Result.success(Unit)
        override suspend fun sendTimers(timers: TimeConfig) = Result.success(Unit)
        override suspend fun sendSwitches(config: SwitchConfig) = Result.success(Unit)
        override suspend fun syncTime() = Result.success(Unit)
        override suspend fun sendRaw(bytes: ByteArray): Result<Unit> {
            sentRaw.add(bytes)
            return sendResult
        }
    }

    private lateinit var viewModel: DebugViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = DebugViewModel(fakeClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `帧日志流投影到状态`() {
        logs.value = listOf(
            FrameLog(1L, FrameLog.Direction.TX, byteArrayOf(0x23), "QUERY"),
        )

        assertEquals(1, viewModel.uiState.value.logs.size)
    }

    @Test
    fun `输入实时校验`() {
        viewModel.onInputChange("23-00")
        assertEquals(true, viewModel.uiState.value.inputError)

        viewModel.onInputChange("23-00-00-00-00-00-00-00-00-00-00-00-00-E8")
        assertEquals(false, viewModel.uiState.value.inputError)

        viewModel.onInputChange("")
        assertNull(viewModel.uiState.value.inputError)
    }

    @Test
    fun `合法输入发送原始帧`() {
        viewModel.onInputChange("23-00-00-00-00-00-00-00-00-00-00-00-00-E8")

        viewModel.onSend()

        val expected = HexFormat.of().parseHex("23000000000000000000000000E8")
        org.junit.Assert.assertArrayEquals(expected, sentRaw.single())
    }

    @Test
    fun `非法输入不发送并标错`() {
        viewModel.onInputChange("nope")

        viewModel.onSend()

        assertEquals(0, sentRaw.size)
        assertEquals(true, viewModel.uiState.value.inputError)
    }

    @Test
    fun `发送失败发SendFailed副作用`() {
        viewModel.onInputChange("23-00-00-00-00-00-00-00-00-00-00-00-00-E8")
        sendResult = Result.failure(IllegalStateException("not connected"))

        val effects = mutableListOf<DebugSideEffect>()
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        val job = scope.launch { viewModel.sideEffect.collect { effects.add(it) } }
        viewModel.onSend()
        job.cancel()

        assertEquals(1, effects.count { it == DebugSideEffect.SendFailed })
    }
}
