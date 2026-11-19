// Envelopes list: an envelope's progress bar and the lines under it, as `envelope_progress` returns them.

package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_contributing
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_overspent
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_paid
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_pending
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_remaining
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_next
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_pays_on
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_reserved_of
import dev.lacruzjs.sobreio.generated.resources.envelope_progress_then
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import kotlin.math.abs
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.EnvelopeProgress
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/** The thin bar and the muted line under an envelope's name; nothing when there is nothing to measure. */
@Composable
internal fun EnvelopeProgressView(progress: EnvelopeProgress, currencyCode: String, languageTag: String) {
    if (!progress.hasBar) {
        // Nothing to measure, but a pending due date still deserves its line
        if (progress.pending > 0) ProgressLine(progress, currencyCode, languageTag)
        return
    }
    val colors = LocalMoneyColors.current
    val fill = when (progress.state) {
        ProgressState.Remaining -> MaterialTheme.colorScheme.primary
        ProgressState.Overspent -> colors.negative
        else -> colors.inflow
    }
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (progress.fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(fill),
            )
        }
    }
    ProgressLine(progress, currencyCode, languageTag)
}

@Composable
private fun ProgressLine(progress: EnvelopeProgress, currencyCode: String, languageTag: String) {
    if (progress.state == ProgressState.Reserving || progress.state == ProgressState.Paid) {
        BillsLines(progress, currencyCode, languageTag)
        return
    }
    val colors = LocalMoneyColors.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val money = { cents: Long -> formatMoney(cents, currencyCode, languageTag) }
    val (text, tone) = when (progress.state) {
        ProgressState.Remaining -> stringResource(
            Res.string.envelope_progress_remaining, money(progress.currentCents), money(progress.goalCents),
        ) to muted
        // Bills have their own two lines
        ProgressState.Reserving, ProgressState.Paid -> "" to muted
        ProgressState.Contributing -> stringResource(
            Res.string.envelope_progress_contributing, money(progress.currentCents), money(progress.goalCents),
        ) to muted
        ProgressState.Overspent -> stringResource(Res.string.envelope_progress_overspent, money(abs(progress.currentCents))) to colors.negative
        ProgressState.None -> "" to muted
    }
    val pending = if (progress.pending > 0) stringResource(Res.string.envelope_progress_pending, progress.pending) else null
    val separator = stringResource(Res.string.row_subtitle, "", "")
    Text(
        text = buildAnnotatedString {
            if (text.isNotEmpty()) withStyle(SpanStyle(color = tone)) { append(text) }
            if (pending != null) {
                if (text.isNotEmpty()) withStyle(SpanStyle(color = muted)) { append(separator) }
                withStyle(SpanStyle(color = muted)) { append(pending) }
            }
        },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * The two lines of a bills envelope, both on one row each so nothing wraps: what is reserved for the next payment, and
 * when it is due (or that it is paid) with the payment that follows and the pending count.
 */
@Composable
private fun BillsLines(progress: EnvelopeProgress, currencyCode: String, languageTag: String) {
    val colors = LocalMoneyColors.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val currentYear = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
    val day = { date: kotlinx.datetime.LocalDate -> formatShortDate(date, languageTag, currentYear) }
    val separator = stringResource(Res.string.row_subtitle, "", "")
    val paid = progress.state == ProgressState.Paid
    if (!paid) {
        Text(
            text = stringResource(
                Res.string.envelope_progress_reserved_of,
                formatMoney(progress.currentCents, currencyCode, languageTag),
                formatMoney(progress.goalCents, currencyCode, languageTag),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    val head = if (paid) {
        progress.paidOn?.let { stringResource(Res.string.envelope_progress_paid, day(it)) }
    } else {
        progress.nextDue?.let { stringResource(Res.string.envelope_progress_pays_on, day(it)) }
    }
    val following = progress.followingDue?.let {
        stringResource(if (paid) Res.string.envelope_progress_next else Res.string.envelope_progress_then, day(it))
    }
    val pending = if (progress.pending > 0) stringResource(Res.string.envelope_progress_pending, progress.pending) else null
    if (head == null && following == null && pending == null) return
    Text(
        text = buildAnnotatedString {
            var first = true
            fun part(text: String?, tone: androidx.compose.ui.graphics.Color) {
                if (text == null) return
                if (!first) withStyle(SpanStyle(color = muted)) { append(separator) }
                withStyle(SpanStyle(color = tone)) { append(text) }
                first = false
            }
            part(head, if (paid) colors.inflow else muted)
            part(following, muted)
            part(pending, muted)
        },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = if (paid) 4.dp else 0.dp),
    )
}
