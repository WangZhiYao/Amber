package cn.floriax.amber.domain.device.model

/**
 * Frame log entry (debug panel data). [bytes] is the raw frame (null for
 * SYS events); the UI layer resolves the frame-kind label from the bytes.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class FrameLog(
    val timestamp: Long,
    val direction: Direction,
    val bytes: ByteArray?,
    val text: String,
) {
    enum class Direction { TX, RX, SYS }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameLog) return false
        return timestamp == other.timestamp && direction == other.direction &&
                text == other.text && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
