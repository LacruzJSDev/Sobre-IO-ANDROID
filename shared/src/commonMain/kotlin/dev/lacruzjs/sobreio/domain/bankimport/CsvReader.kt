package dev.lacruzjs.sobreio.domain.bankimport

/** The separators a bank CSV may use, most usual first in Spain. */
internal val CANDIDATE_SEPARATORS = listOf(';', ',', '\t', '|')

/**
 * The separator of a CSV: the candidate that splits the first lines into the same number of cells (more than
 * one) most often, counting only separators outside quotes. Ties go to the earlier candidate, so `;` wins.
 */
fun detectSeparator(text: String): Char {
    val lines = text.lineSequence().filter { it.isNotBlank() }.take(20).toList()
    if (lines.isEmpty()) return ';'
    var best = ';'
    var bestScore = -1
    for (candidate in CANDIDATE_SEPARATORS) {
        val counts = lines.map { countOutsideQuotes(it, candidate) }
        val mostCommon = counts.filter { it > 0 }.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: continue
        // Lines that agree on the count, weighted a little by how many cells they make
        val score = mostCommon.value * 100 + mostCommon.key
        if (score > bestScore) {
            bestScore = score
            best = candidate
        }
    }
    return best
}

private fun countOutsideQuotes(line: String, separator: Char): Int {
    var inQuotes = false
    var count = 0
    for (c in line) {
        if (c == '"') inQuotes = !inQuotes else if (c == separator && !inQuotes) count++
    }
    return count
}

/**
 * Splits CSV text into rows of cells (RFC 4180: quoted cells may hold the separator, line breaks and doubled
 * quotes). Cells are trimmed; rows with no content are dropped.
 */
fun parseCsv(text: String, separator: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    fun endCell() {
        row.add(cell.toString().trim())
        cell.clear()
    }
    fun endRow() {
        endCell()
        if (row.any { it.isNotEmpty() }) rows.add(row)
        row = mutableListOf()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                cell.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            inQuotes -> cell.append(c)
            c == separator -> endCell()
            c == '\r' -> Unit
            c == '\n' -> endRow()
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}
