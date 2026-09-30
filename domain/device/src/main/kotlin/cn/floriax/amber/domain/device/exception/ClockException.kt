package cn.floriax.amber.domain.device.exception

/**
 * The only failure shape of clock repository operations: the device is not
 * connected ([NotConnected]) or the operation failed mid-way ([Failed],
 * carrying the original cause). Repository suspend methods never throw
 * business exceptions — failures are reported via kotlin.Result with this type.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
sealed class ClockException : Exception {

    constructor() : super()

    constructor(message: String, cause: Throwable?) : super(message, cause)

    /** Device not connected (never connected, disconnected, or handshake pending). */
    class NotConnected : ClockException() {
        private fun readResolve(): Any = NotConnected()
    }

    /** Operation failed; cause is the original exception. */
    class Failed(cause: Throwable) :
        ClockException("Operation failed: ${cause.message}", cause)
}
