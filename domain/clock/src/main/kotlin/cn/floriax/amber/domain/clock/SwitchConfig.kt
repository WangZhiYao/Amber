package cn.floriax.amber.domain.clock

/**
 * Function switches: timer enables, hour format, colon blink, mute and
 * remote lock.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class SwitchConfig(
    val powerOffEnabled: Boolean = false,
    val powerOnEnabled: Boolean = false,
    val alarmEnabled: Boolean = false,
    val hour12: Boolean = false,
    val colonBlink: Boolean = false,
    val mute: Boolean = false,
    val lockRemote: Boolean = false,
)
