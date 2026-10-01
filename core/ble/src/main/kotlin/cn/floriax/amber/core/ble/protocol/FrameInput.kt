package cn.floriax.amber.core.ble.protocol

import java.util.HexFormat

/**
 * Parses manual frame input from the debug panel.
 *
 * Accepts `XX-XX`, `XX XX` and bare `XXXX` forms (leading/trailing
 * whitespace tolerated) and returns the 14 bytes when the hex is exactly
 * 14 bytes **and** the head/tail pair matches a known frame kind — null
 * otherwise. Frame validation belongs to the protocol layer so the
 * parser stays testable next to the frames themselves.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
fun parseFrameInput(input: String): ByteArray? {
    val cleaned = input.trim().replace("-", "").replace(" ", "")
    if (cleaned.length != FRAME_HEX_LENGTH) return null
    if (cleaned.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
    val bytes = runCatching { HexFormat.of().parseHex(cleaned) }.getOrNull() ?: return null
    return bytes.takeIf { FrameKind.of(it) != null }
}

private const val FRAME_HEX_LENGTH = 14 * 2
