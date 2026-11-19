package dev.lacruzjs.sobreio.domain.export

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportCsvTest {

    private val es = CsvDialect.forLanguage("es-ES")
    private val en = CsvDialect.forLanguage("en")

    @Test
    fun spanishUsesSemicolonsAndADecimalComma() {
        assertEquals(CsvDialect(';', true), es)
        assertEquals(CsvDialect(',', false), en)
        assertEquals("-1234,05", csvAmount(-123_405, es))
        assertEquals("0.07", csvAmount(7, en))
        assertEquals("12.00", csvAmount(1_200, en))
    }

    @Test
    fun fieldsWithTheSeparatorQuotesOrLineBreaksAreQuoted() {
        assertEquals("plain", csvField("plain", es))
        assertEquals("\"a;b\"", csvField("a;b", es))
        assertEquals("a;b", csvField("a;b", en))
        assertEquals("\"say \"\"hi\"\"\"", csvField("say \"hi\"", en))
        assertEquals("\"two\nlines\"", csvField("two\nlines", en))
    }

    @Test
    fun theFileStartsWithABomAndEndsLinesWithCrLf() {
        val csv = buildCsv(listOf("A", "B"), listOf(listOf("1", "2")), en)
        assertTrue(csv.startsWith("﻿"))
        assertEquals("﻿A,B\r\n1,2\r\n", csv)
    }

    @Test
    fun everyRowBecomesTenColumnsWithItsTypeTranslated() {
        val row = ExportRow(
            ExportRecordType.Transaction, LocalDate(2026, 11, 3), "expense", -4_510,
            "Bank", null, "Food", null, "Market; fruit", "Home", "Ana",
        )
        val csv = buildExportCsv(listOf(row), (1..10).map { "h$it" }, { if (it.kind == "expense") "Expense" else it.kind }, es)
        assertEquals("2026-11-03;Expense;-45,10;Bank;;Food;;\"Market; fruit\";Home;Ana", csv.lines()[1].trimEnd('\r'))
    }

    @Test
    fun theFileNameIsSafeAndSaysThePeriod() {
        val period = ExportPeriod("p1", LocalDate(2026, 10, 25), LocalDate(2026, 11, 24))
        assertEquals("sobreio-casa-y-coche-2026-10-25_2026-11-24.csv", exportFileName("Casa y coche!", period))
        assertEquals("sobreio-group-2026-10-25_2026-11-24.csv", exportFileName("  ", period))
    }
}
