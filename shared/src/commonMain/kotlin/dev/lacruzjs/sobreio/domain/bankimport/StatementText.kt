package dev.lacruzjs.sobreio.domain.bankimport

/**
 * Turns the bytes of a bank statement into text. Spanish banks export either UTF-8 or Windows-1252, and
 * reading Windows-1252 as UTF-8 breaks every accent, so the bytes are read as UTF-8 only when they are
 * valid UTF-8 and as Windows-1252 otherwise. A UTF-8 byte order mark is dropped.
 */
fun decodeStatement(bytes: ByteArray): String {
    val start = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
    val body = if (start == 0) bytes else bytes.copyOfRange(start, bytes.size)
    return if (isValidUtf8(body)) body.decodeToString() else decodeWindows1252(body)
}

/** True when every byte sequence is a well formed UTF-8 character. */
internal fun isValidUtf8(bytes: ByteArray): Boolean {
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xFF
        val extra = when {
            b < 0x80 -> 0
            b in 0xC2..0xDF -> 1
            b in 0xE0..0xEF -> 2
            b in 0xF0..0xF4 -> 3
            else -> return false
        }
        if (i + extra >= bytes.size && extra > 0) return false
        for (k in 1..extra) {
            if ((bytes[i + k].toInt() and 0xC0) != 0x80) return false
        }
        i += extra + 1
    }
    return true
}

/** Windows-1252 positions 0x80..0x9F, the only part that differs from Latin-1 (0 = unused). */
private val cp1252High = intArrayOf(
    0x20AC, 0, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0, 0x017D, 0,
    0, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0, 0x017E, 0x0178,
)

internal fun decodeWindows1252(bytes: ByteArray): String = buildString(bytes.size) {
    for (byte in bytes) {
        val b = byte.toInt() and 0xFF
        val code = if (b in 0x80..0x9F) cp1252High[b - 0x80].takeIf { it != 0 } ?: 0xFFFD else b
        append(code.toChar())
    }
}
