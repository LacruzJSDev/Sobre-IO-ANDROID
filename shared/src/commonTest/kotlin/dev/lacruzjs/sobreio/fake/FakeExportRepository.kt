package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.ExportRepository
import dev.lacruzjs.sobreio.domain.export.ExportRow

/** Scripted exports; [myDataCalls] counts the reads of "Download my data". */
class FakeExportRepository(
    var rows: Result<List<ExportRow>> = Result.success(emptyList()),
    var myData: Result<String> = Result.success("""{"format":"sobre-io-export"}"""),
) : ExportRepository {

    var myDataCalls = 0

    override suspend fun exportPeriod(groupId: String, periodId: String) = rows
    override suspend fun exportMyData(): Result<String> {
        myDataCalls++
        return myData
    }
}
