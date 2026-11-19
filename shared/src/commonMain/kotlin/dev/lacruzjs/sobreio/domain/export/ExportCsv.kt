package dev.lacruzjs.sobreio.domain.export

import kotlinx.datetime.LocalDate
import kotlin.math.absoluteValue

/** One period the group already has, to export (`periods`). */
data class ExportPeriod(val id: String, val start: LocalDate, val end: LocalDate)

/** What a row of `export_period` is. */
enum class ExportRecordType { Transaction, AccountTransfer, EnvelopeTransfer }

/**
 * One row of `export_period` (D-36): a live movement of the period with readable names. [amountCents] is signed for a
 * transaction (income positive), the transferred amount for a transfer. [kind] is the database's own word (`expense`,
 * `income`, `adjustment`, `group_transfer_in`, `transfer`, an envelope transfer's origin…); the screen translates it.
 */
data class ExportRow(
    val recordType: ExportRecordType,
    val date: LocalDate,
    val kind: String,
    val amountCents: Long,
    val account: String?,
    val toAccount: String?,
    val envelope: String?,
    val toEnvelope: String?,
    val notes: String?,
    val tags: String?,
    val author: String?,
)

/**
 * How the file is written for the user's language: spreadsheets in Spanish expect `;` between columns and a decimal
 * comma; others `,` and a decimal point.
 */
data class CsvDialect(val separator: Char, val decimalComma: Boolean) {
    companion object {
        fun forLanguage(languageTag: String): CsvDialect =
            if (languageTag.lowercase().startsWith("es")) CsvDialect(';', true) else CsvDialect(',', false)
    }
}

/** Cents as a plain number a spreadsheet reads: `-1234.50` or `-1234,50`. No currency symbol, no thousands mark. */
fun csvAmount(cents: Long, dialect: CsvDialect): String {
    val sign = if (cents < 0) "-" else ""
    val abs = cents.absoluteValue
    val mark = if (dialect.decimalComma) ',' else '.'
    return "$sign${abs / 100}$mark${(abs % 100).toString().padStart(2, '0')}"
}

/** A field, quoted when it holds the separator, a quote or a line break (quotes doubled). */
fun csvField(value: String, dialect: CsvDialect): String {
    val needsQuotes = value.any { it == dialect.separator || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
}

/**
 * The whole file: a byte order mark (so spreadsheets read UTF-8 accents), the [header] line and one line per row, with
 * CRLF line ends as RFC 4180 says.
 */
fun buildCsv(header: List<String>, rows: List<List<String>>, dialect: CsvDialect): String = buildString {
    append('﻿')
    (listOf(header) + rows).forEach { cells ->
        append(cells.joinToString(dialect.separator.toString()) { csvField(it, dialect) })
        append("\r\n")
    }
}

/** `sobreio-<group>-<start>_<end>.csv`, with the group name reduced to safe letters. */
fun exportFileName(groupName: String, period: ExportPeriod): String {
    val slug = groupName.lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .split('-').filter { it.isNotEmpty() }.joinToString("-")
        .ifEmpty { "group" }
    return "sobreio-$slug-${period.start}_${period.end}.csv"
}

/**
 * The export file of [rows]: the given [header] (already translated, ten columns: date, type, amount, account, to
 * account, envelope, to envelope, notes, tags, author) and one line per row, its type translated by [kindLabel].
 */
fun buildExportCsv(
    rows: List<ExportRow>,
    header: List<String>,
    kindLabel: (ExportRow) -> String,
    dialect: CsvDialect,
): String = buildCsv(
    header = header,
    rows = rows.map { row ->
        listOf(
            row.date.toString(),
            kindLabel(row),
            csvAmount(row.amountCents, dialect),
            row.account.orEmpty(),
            row.toAccount.orEmpty(),
            row.envelope.orEmpty(),
            row.toEnvelope.orEmpty(),
            row.notes.orEmpty(),
            row.tags.orEmpty(),
            row.author.orEmpty(),
        )
    },
    dialect = dialect,
)
