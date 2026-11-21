package dev.lacruzjs.sobreio.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import dev.lacruzjs.sobreio.domain.model.AuditAction
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.history_automatic
import dev.lacruzjs.sobreio.generated.resources.history_changed
import dev.lacruzjs.sobreio.generated.resources.history_created
import dev.lacruzjs.sobreio.generated.resources.history_field_account
import dev.lacruzjs.sobreio.generated.resources.history_field_amount
import dev.lacruzjs.sobreio.generated.resources.history_field_date
import dev.lacruzjs.sobreio.generated.resources.history_field_details
import dev.lacruzjs.sobreio.generated.resources.history_field_envelope
import dev.lacruzjs.sobreio.generated.resources.history_field_notes
import dev.lacruzjs.sobreio.generated.resources.history_field_voided
import dev.lacruzjs.sobreio.generated.resources.history_title
import dev.lacruzjs.sobreio.generated.resources.member_former
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.format.formatDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Bookkeeping columns every change touches: they say nothing to the user. */
private val IGNORED_FIELDS = setOf("updated_at", "updated_by", "created_at", "created_by")

/** What a changed column is called in the history line; columns without a name of their own are "details". */
internal fun historyFieldRes(field: String): StringResource = when (field) {
    "amount" -> Res.string.history_field_amount
    "date" -> Res.string.history_field_date
    "notes" -> Res.string.history_field_notes
    "envelope_id" -> Res.string.history_field_envelope
    "account_id", "from_account_id", "to_account_id" -> Res.string.history_field_account
    "voided_at" -> Res.string.history_field_voided
    else -> Res.string.history_field_details
}

/** The changed columns the user cares about, each named once, in a stable order. */
internal fun visibleHistoryFields(fields: List<String>): List<StringResource> =
    fields.filterNot { it in IGNORED_FIELDS }.map(::historyFieldRes).distinct()

/**
 * The end of an edit form: "Created by Ana · 12 Nov 2026" and one line per later change ("Juan changed amount, date ·
 * 14 Nov 2026"). Hidden when there is nothing to show.
 */
@Composable
fun MovementHistorySection(lines: List<HistoryLine>) {
    if (lines.isEmpty()) return
    val languageTag = Locale.current.toLanguageTag()
    val former = stringResource(Res.string.member_former)
    val automatic = stringResource(Res.string.history_automatic)
    FormSection(title = stringResource(Res.string.history_title), segmented = false) {
        lines.forEach { line ->
            val who = when {
                line.entry.changedBy == null -> automatic
                else -> line.authorName ?: former
            }
            val day = formatDate(line.entry.changedAt, languageTag)
            val text = when (line.entry.action) {
                AuditAction.Created -> stringResource(Res.string.history_created, who, day)
                AuditAction.Changed -> {
                    val fields = visibleHistoryFields(line.entry.fields).map { stringResource(it) }
                    if (fields.isEmpty()) return@forEach
                    stringResource(Res.string.history_changed, who, fields.joinToString(", "), day)
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
