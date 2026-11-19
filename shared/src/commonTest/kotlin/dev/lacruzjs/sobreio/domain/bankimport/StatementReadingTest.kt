package dev.lacruzjs.sobreio.domain.bankimport

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatementReadingTest {

    @Test
    fun utf8IsReadAsUtf8AndTheBomIsDropped() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Línea".encodeToByteArray()
        assertEquals("Línea", decodeStatement(bytes))
    }

    @Test
    fun windows1252IsReadWhenTheBytesAreNotUtf8() {
        // "Línea €" in Windows-1252: í = 0xED, € = 0x80
        val bytes = byteArrayOf('L'.code.toByte(), 0xED.toByte(), 'n'.code.toByte(), 'e'.code.toByte(), 'a'.code.toByte(), ' '.code.toByte(), 0x80.toByte())
        assertFalse(isValidUtf8(bytes))
        assertEquals("Línea €", decodeStatement(bytes))
    }

    @Test
    fun separatorIsTheOneThatSplitsLinesAlike() {
        assertEquals(';', detectSeparator("Fecha;Concepto;Importe\n01/11/2026;\"Bar, café\";-1,50\n"))
        assertEquals(',', detectSeparator("Date,Description,Amount\n2026-11-01,Coffee,-1.50\n"))
        assertEquals('\t', detectSeparator("Date\tDescription\tAmount\n2026-11-01\tCoffee\t-1.50\n"))
    }

    @Test
    fun csvKeepsQuotedSeparatorsQuotesAndLineBreaks() {
        val rows = parseCsv("a;\"b;c\";\"say \"\"hi\"\"\"\r\n\"multi\nline\";x;y\n\n", ';')
        assertEquals(listOf(listOf("a", "b;c", "say \"hi\""), listOf("multi\nline", "x", "y")), rows)
    }

    @Test
    fun datesInEveryOrder() {
        assertEquals(LocalDate(2026, 11, 14), parseStatementDate("14/11/2026", DateOrder.DayMonthYear))
        assertEquals(LocalDate(2026, 11, 14), parseStatementDate("14-11-26", DateOrder.DayMonthYear))
        assertEquals(LocalDate(2026, 11, 14), parseStatementDate("11/14/2026", DateOrder.MonthDayYear))
        assertEquals(LocalDate(2026, 11, 14), parseStatementDate("2026-11-14", DateOrder.YearMonthDay))
        assertNull(parseStatementDate("31/02/2026", DateOrder.DayMonthYear))
        assertNull(parseStatementDate("MERCADONA", DateOrder.DayMonthYear))
    }

    @Test
    fun dateOrderIsGuessedAndAmbiguityReported() {
        assertEquals(DateOrderGuess(DateOrder.DayMonthYear, false), guessDateOrder(listOf("01/11/2026", "14/11/2026")))
        assertEquals(DateOrderGuess(DateOrder.MonthDayYear, false), guessDateOrder(listOf("11/01/2026", "11/14/2026")))
        assertEquals(DateOrderGuess(DateOrder.YearMonthDay, false), guessDateOrder(listOf("2026-11-01")))
        assertEquals(DateOrderGuess(DateOrder.DayMonthYear, true), guessDateOrder(listOf("01/11/2026", "05/11/2026")))
    }

    @Test
    fun amountsInSpanishAndEnglishFormats() {
        assertEquals(-4230L, parseStatementAmount("-42,30", DecimalMark.Comma))
        assertEquals(129815L, parseStatementAmount("1.298,15 €", DecimalMark.Comma))
        assertEquals(129815L, parseStatementAmount("€1,298.15", DecimalMark.Dot))
        assertEquals(-150L, parseStatementAmount("(1.50)", DecimalMark.Dot))
        assertEquals(-5000L, parseStatementAmount("50,00-", DecimalMark.Comma))
        assertEquals(-5000L, parseStatementAmount("−50 EUR", DecimalMark.Comma))
        assertEquals(1500L, parseStatementAmount("+15", DecimalMark.Comma))
        assertEquals(10L, parseStatementAmount("0,1", DecimalMark.Comma))
        assertNull(parseStatementAmount("abc", DecimalMark.Comma))
        assertNull(parseStatementAmount("1,234", DecimalMark.Comma))
        assertNull(parseStatementAmount("", DecimalMark.Comma))
    }

    @Test
    fun decimalMarkIsGuessedFromTheLastMark() {
        assertEquals(DecimalMark.Comma, guessDecimalMark(listOf("-42,30", "1.208,15")).mark)
        assertEquals(DecimalMark.Dot, guessDecimalMark(listOf("-42.30", "1,208.15")).mark)
        assertTrue(guessDecimalMark(listOf("-42", "15")).ambiguous)
    }

    private val spanishStatement = """
        Cuenta;ES00 0000
        Periodo;01/11/2026 - 17/11/2026

        F.Operativa;Concepto;F.Valor;Importe;Saldo
        14/11/2026;MERCADONA VALENCIA;14/11/2026;-42,30;1.208,15
        14/11/2026;CAFE;14/11/2026;-1,50;1.250,45
        14/11/2026;CAFE;14/11/2026;-1,50;1.251,95
        10/11/2026;NOMINA ACME;10/11/2026;1.500,00;1.253,45
    """.trimIndent()

    @Test
    fun mappingIsGuessedPastThePreamble() {
        val table = StatementTable("extracto.csv", parseCsv(spanishStatement, detectSeparator(spanishStatement)))
        val mapping = assertNotNull(guessMapping(table))
        assertEquals(2, mapping.headerRow)
        assertEquals(0, mapping.dateColumn)
        assertEquals(1, mapping.descriptionColumn)
        assertEquals(AmountMode.OneColumn, mapping.amountMode)
        assertEquals(3, mapping.amountColumn)
        assertEquals(4, mapping.balanceColumn)
        assertEquals(DecimalMark.Comma, mapping.decimalMark)
        assertTrue(mapping.isComplete(table.columnCount))
    }

    @Test
    fun rowsGetStableAndDistinctReferencesAndTheClosingBalance() {
        val table = StatementTable("extracto.csv", parseCsv(spanishStatement, ';'))
        val mapping = assertNotNull(guessMapping(table))
        val read = readStatementRows(table, mapping)
        assertEquals(4, read.rows.size)
        assertEquals(0, read.skipped)
        val (first, coffee1, coffee2) = read.rows
        assertEquals(-4230L, first.amountCents)
        assertEquals("MERCADONA VALENCIA", first.description)
        assertNotEquals(coffee1.ref, coffee2.ref)
        // Same file read again gives the same references
        assertEquals(read.rows.map { it.ref }, readStatementRows(table, mapping).rows.map { it.ref })
        // Newest first: the closing balance is the first row of the newest day
        assertEquals(120815L, read.closingBalanceCents)
        assertEquals(LocalDate(2026, 11, 10), read.firstDate)
    }

    @Test
    fun chargeAndCreditColumnsGiveSignedAmounts() {
        val csv = "Date,Description,Debit,Credit\n2026-11-01,Rent,650.00,\n2026-11-02,Refund,,12.40\n2026-11-03,Nothing,,\n"
        val table = StatementTable("s.csv", parseCsv(csv, ','))
        val mapping = assertNotNull(guessMapping(table))
        assertEquals(AmountMode.ChargeAndCredit, mapping.amountMode)
        val read = readStatementRows(table, mapping)
        assertEquals(listOf(-65000L, 1240L), read.rows.map { it.amountCents })
        assertEquals(1, read.skipped)
    }

    @Test
    fun aFileWithoutHeaderStartsWithData() {
        val csv = "01/11/2026;Coffee;-1,50\n02/11/2026;Bread;-2,10\n"
        val table = StatementTable("s.csv", parseCsv(csv, ';'))
        val mapping = assertNotNull(guessMapping(table))
        assertEquals(-1, mapping.headerRow)
        assertEquals(2, readStatementRows(table, mapping).rows.size)
    }

    @Test
    fun headerSignatureIgnoresCaseAndPunctuation() {
        assertEquals(headerSignature(listOf("F.Valor", "Concepto ")), headerSignature(listOf("f valor", "CONCEPTO")))
    }

    @Test
    fun aReferenceIsAFixedHashOfTheRowAndItsOccurrence() {
        // FNV-1a 64 of "base#1": the same on every device and every version, so a re-import finds its rows (D-37)
        assertEquals("b1:49c5afa0f49fb4b0", statementRef("base", 1))
        assertNotEquals(statementRef("base", 1), statementRef("base", 2))
    }
}
