package dev.lacruzjs.sobreio.domain.bankimport

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** How the amount comes: one signed column, or charges and credits in two columns. */
enum class AmountMode { OneColumn, ChargeAndCredit }

/**
 * Which column holds what (D-37: the app maps the CSV). Column indexes are 0-based; [headerRow] is the row
 * with the column names (-1 when the file has none), rows above it (bank preambles) are skipped; -1 also
 * means "none" for the optional columns. Remembered per account and header.
 */
@Serializable
data class ColumnMapping(
    val headerRow: Int,
    val dateColumn: Int,
    val descriptionColumn: Int,
    val amountMode: AmountMode,
    val amountColumn: Int,
    val chargeColumn: Int = -1,
    val creditColumn: Int = -1,
    val balanceColumn: Int = -1,
    val dateOrder: DateOrder,
    val decimalMark: DecimalMark,
)

/** The statement read as a table: every CSV row, with the header row chosen in the mapping. */
data class StatementTable(val fileName: String, val rows: List<List<String>>) {

    fun header(headerRow: Int): List<String> = rows.getOrNull(headerRow).orEmpty()

    fun body(headerRow: Int): List<List<String>> = rows.drop(headerRow + 1)

    val columnCount: Int get() = rows.maxOfOrNull { it.size } ?: 0
}

/** One statement row ready for the preview: [ref] is the stable reference the database keeps (D-37). */
data class StatementRow(
    val index: Int,
    val ref: String,
    val date: LocalDate,
    val amountCents: Long,
    val description: String,
    val balanceCents: Long?,
)

/** The rows that could be read and how many could not (no date, no amount or an amount of 0). */
data class StatementRows(val rows: List<StatementRow>, val skipped: Int) {

    /**
     * The bank balance at the end of the statement, when it has a balance column: the balance of the newest
     * row. Banks list newest first or oldest first; within the newest day the row the bank wrote last wins.
     */
    val closingBalanceCents: Long? get() {
        val withBalance = rows.filter { it.balanceCents != null }
        if (withBalance.isEmpty()) return null
        val newestFirst = rows.first().date >= rows.last().date
        val newestDay = withBalance.maxOf { it.date }
        val sameDay = withBalance.filter { it.date == newestDay }
        return (if (newestFirst) sameDay.first() else sameDay.last()).balanceCents
    }

    val firstDate: LocalDate? get() = rows.minOfOrNull { it.date }

    val lastDate: LocalDate? get() = rows.maxOfOrNull { it.date }
}

private val headerWords = Regex("""[^\p{L}\p{N}]+""")

/** The header as a key to remember the mapping: lower case words, joined. Same bank format, same key. */
fun headerSignature(header: List<String>): String =
    header.joinToString("|") { cell -> cell.lowercase().split(headerWords).filter { it.isNotEmpty() }.joinToString(" ") }

private val dateHints = listOf("fecha", "date", "operativa", "operación", "operacion", "booking")
private val descriptionHints = listOf("concepto", "descripción", "descripcion", "description", "movimiento", "detalle", "details", "payee", "observaciones")
private val amountHints = listOf("importe", "amount", "cantidad")
private val chargeHints = listOf("cargo", "debe", "debit", "débito", "debito", "gasto", "withdrawal")
private val creditHints = listOf("abono", "haber", "credit", "crédito", "credito", "ingreso", "deposit")
private val balanceHints = listOf("saldo", "balance", "disponible")

private fun String.hasHint(hints: List<String>): Boolean {
    val text = lowercase()
    return hints.any { it in text }
}

/**
 * Finds the header row and guesses which column holds what. The header is the first row followed by a row
 * with a date; columns are chosen by their values first (most dates, most amounts) and their names second.
 * Returns null when the statement has no column of dates or of amounts: the user maps it by hand.
 */
fun guessMapping(table: StatementTable): ColumnMapping? {
    fun hasDate(row: List<String>) = row.any { cell -> DateOrder.entries.any { parseStatementDate(cell, it) != null } }
    // A file whose first row is already data has no header (-1)
    // The header has no dates and at least three names, and the row under it has a date (bank preambles have fewer cells)
    val headerRow = if (table.rows.firstOrNull()?.let(::hasDate) == true) -1 else (0 until table.rows.size - 1).firstOrNull { r ->
        !hasDate(table.rows[r]) && table.rows[r].count { it.isNotBlank() } >= 3 && hasDate(table.rows[r + 1])
    } ?: return null
    val header = table.header(headerRow)
    val body = table.body(headerRow).take(50)
    if (body.isEmpty()) return null
    val columns = 0 until table.columnCount
    fun values(column: Int) = body.mapNotNull { it.getOrNull(column)?.takeIf { v -> v.isNotBlank() } }

    val dateScores = columns.associateWith { c -> values(c).count { v -> DateOrder.entries.any { parseStatementDate(v, it) != null } } }
    val dateColumn = columns.filter { (dateScores[it] ?: 0) * 2 >= body.size }
        .sortedWith(compareByDescending<Int> { header.getOrNull(it).orEmpty().hasHint(dateHints) }.thenBy { it })
        .firstOrNull() ?: return null
    val dateColumns = columns.filter { (dateScores[it] ?: 0) * 2 >= body.size }.toSet()
    val dateOrder = guessDateOrder(values(dateColumn)).order

    val markGuess = guessDecimalMark(columns.filter { it !in dateColumns }.flatMap { values(it) })
    val mark = markGuess.mark
    val amountColumns = columns.filter { c ->
        c !in dateColumns && values(c).isNotEmpty() && values(c).all { parseStatementAmount(it, mark) != null }
    }
    if (amountColumns.isEmpty()) return null
    val name = { c: Int -> header.getOrNull(c).orEmpty() }
    val balanceColumn = amountColumns.firstOrNull { name(it).hasHint(balanceHints) } ?: -1
    val charge = amountColumns.firstOrNull { name(it).hasHint(chargeHints) }
    val credit = amountColumns.firstOrNull { name(it).hasHint(creditHints) && it != charge }
    val single = amountColumns.filter { it != balanceColumn }
        .sortedWith(compareByDescending<Int> { name(it).hasHint(amountHints) }.thenBy { it })
        .firstOrNull()
    val textColumns = columns.filter { it !in dateColumns && it !in amountColumns }
    val descriptionColumn = textColumns.firstOrNull { name(it).hasHint(descriptionHints) }
        ?: textColumns.maxByOrNull { c -> values(c).sumOf { it.length } }
        ?: return null

    return if (charge != null && credit != null) {
        ColumnMapping(
            headerRow = headerRow, dateColumn = dateColumn, descriptionColumn = descriptionColumn,
            amountMode = AmountMode.ChargeAndCredit, amountColumn = -1, chargeColumn = charge, creditColumn = credit,
            balanceColumn = balanceColumn, dateOrder = dateOrder, decimalMark = mark,
        )
    } else {
        ColumnMapping(
            headerRow = headerRow, dateColumn = dateColumn, descriptionColumn = descriptionColumn,
            amountMode = AmountMode.OneColumn, amountColumn = single ?: return null,
            balanceColumn = balanceColumn, dateOrder = dateOrder, decimalMark = mark,
        )
    }
}

/** True when the mapping names every column it needs and each exists in the table. */
fun ColumnMapping.isComplete(columnCount: Int): Boolean {
    fun ok(c: Int) = c in 0 until columnCount
    val amounts = when (amountMode) {
        AmountMode.OneColumn -> ok(amountColumn)
        AmountMode.ChargeAndCredit -> ok(chargeColumn) && ok(creditColumn) && chargeColumn != creditColumn
    }
    return ok(dateColumn) && ok(descriptionColumn) && amounts
}

/**
 * Reads the statement rows with [mapping]. A charge counts as negative whatever its sign in the file; a row
 * with no date, no amount or an amount of 0 is skipped and counted. Identical rows (same day, amount and
 * description) get an occurrence number in their reference, so two coffees of 1,50 the same day stay two.
 */
fun readStatementRows(table: StatementTable, mapping: ColumnMapping): StatementRows {
    val rows = mutableListOf<StatementRow>()
    var skipped = 0
    val seen = mutableMapOf<String, Int>()
    table.body(mapping.headerRow).forEachIndexed { i, cells ->
        fun cell(c: Int) = cells.getOrNull(c).orEmpty()
        val date = parseStatementDate(cell(mapping.dateColumn), mapping.dateOrder)
        val amount = when (mapping.amountMode) {
            AmountMode.OneColumn -> parseStatementAmount(cell(mapping.amountColumn), mapping.decimalMark)
            AmountMode.ChargeAndCredit -> {
                val charge = parseStatementAmount(cell(mapping.chargeColumn), mapping.decimalMark)?.let { -kotlin.math.abs(it) } ?: 0L
                val credit = parseStatementAmount(cell(mapping.creditColumn), mapping.decimalMark)?.let { kotlin.math.abs(it) } ?: 0L
                (charge + credit).takeIf { cell(mapping.chargeColumn).isNotBlank() || cell(mapping.creditColumn).isNotBlank() }
            }
        }
        if (date == null || amount == null || amount == 0L) {
            skipped++
            return@forEachIndexed
        }
        val description = cell(mapping.descriptionColumn).replace(Regex("\\s+"), " ").trim()
        val base = "$date|$amount|${description.lowercase()}"
        val occurrence = seen.getOrElse(base) { 0 }
        seen[base] = occurrence + 1
        rows.add(
            StatementRow(
                index = rows.size,
                ref = statementRef(base, occurrence),
                date = date,
                amountCents = amount,
                description = description,
                balanceCents = mapping.balanceColumn.takeIf { it >= 0 }?.let { parseStatementAmount(cell(it), mapping.decimalMark) },
            ),
        )
    }
    return StatementRows(rows, skipped)
}

/** "b1:" and a 64-bit FNV-1a hash of the row and its occurrence, in hex: short, stable and the same on every device. */
internal fun statementRef(base: String, occurrence: Int): String {
    var hash = 0xcbf29ce484222325uL
    for (byte in "$base#$occurrence".encodeToByteArray()) {
        hash = hash xor (byte.toUByte().toULong())
        hash *= 0x100000001b3uL
    }
    return "b1:" + hash.toString(16).padStart(16, '0')
}
