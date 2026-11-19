package dev.lacruzjs.sobreio.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuditRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.model.AuditEntry
import dev.lacruzjs.sobreio.domain.model.MovementSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One line of the history: the entry and the name of who made it (null: the member left, or the daily task). */
data class HistoryLine(val entry: AuditEntry, val authorName: String?)

/**
 * Who created a movement and who changed what since (`audit_log`, D-41), shown at the end of its edit form. Read only;
 * a failure simply leaves the section out.
 */
class MovementHistoryViewModel(
    private val groupId: String,
    private val movementId: String,
    private val source: String?,
    private val audit: AuditRepository,
    private val members: MemberRepository,
) : ViewModel() {

    private val _lines = MutableStateFlow<List<HistoryLine>>(emptyList())
    val lines: StateFlow<List<HistoryLine>> = _lines.asStateFlow()

    init {
        viewModelScope.launch {
            val table = if (source == MovementSource.AccountTransfer.name) "account_transfers" else "transactions"
            val entries = audit.getHistory(table, movementId).getOrNull() ?: return@launch
            val names = members.getMembers(groupId).getOrNull().orEmpty().associate { it.userId to it.displayName }
            _lines.value = entries.map { HistoryLine(it, it.changedBy?.let(names::get)) }
        }
    }
}
