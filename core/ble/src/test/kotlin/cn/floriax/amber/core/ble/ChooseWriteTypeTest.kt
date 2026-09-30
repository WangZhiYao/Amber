package cn.floriax.amber.core.ble

import no.nordicsemi.android.kotlin.ble.core.data.BleGattProperty
import no.nordicsemi.android.kotlin.ble.core.data.BleWriteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 写类型选择单测：按特征实际属性挑选，廉价 fff1/ffe1 串口模块往往
 * 只有 WRITE_WITHOUT_RESPONSE——固定 DEFAULT 会在真机上必然抛
 * MissingPropertyException（连接死循环的根因之一）。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class ChooseWriteTypeTest {

    @Test
    fun `有WRITE属性选确认写`() {
        assertEquals(
            BleWriteType.DEFAULT,
            chooseWriteType(listOf(BleGattProperty.PROPERTY_WRITE, BleGattProperty.PROPERTY_NOTIFY)),
        )
    }

    @Test
    fun `仅WRITE_NO_RESPONSE选无响应写`() {
        assertEquals(
            BleWriteType.NO_RESPONSE,
            chooseWriteType(listOf(BleGattProperty.PROPERTY_WRITE_NO_RESPONSE, BleGattProperty.PROPERTY_NOTIFY)),
        )
    }

    @Test
    fun `两种写属性并存优先确认写`() {
        assertEquals(
            BleWriteType.DEFAULT,
            chooseWriteType(
                listOf(
                    BleGattProperty.PROPERTY_WRITE,
                    BleGattProperty.PROPERTY_WRITE_NO_RESPONSE,
                    BleGattProperty.PROPERTY_NOTIFY,
                ),
            ),
        )
    }

    @Test
    fun `无任何写属性抛BleException`() {
        assertThrows(BleException::class.java) {
            chooseWriteType(listOf(BleGattProperty.PROPERTY_NOTIFY))
        }
    }
}
