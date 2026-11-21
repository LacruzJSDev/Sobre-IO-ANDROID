package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import dev.lacruzjs.sobreio.ui.theme.noticeWarningContainer
import dev.lacruzjs.sobreio.ui.theme.onNoticeWarning

enum class NoticeTone { Info, Warning, Suggestion }

/**
 * A flat tinted block that tells the user something about the form or the movement (a consequence, a state, a
 * suggestion). [FormNote] stays for a one-line caption under a field. [actions] sit at the end, under the text.
 */
@Composable
fun InlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.Info,
    title: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val background = when (tone) {
        NoticeTone.Info -> scheme.primary.copy(alpha = 0.06f)
        NoticeTone.Warning -> scheme.noticeWarningContainer
        NoticeTone.Suggestion -> scheme.primary.copy(alpha = 0.10f)
    }
    val iconColor = if (tone == NoticeTone.Warning) scheme.onNoticeWarning else scheme.onSurfaceVariant
    val icon = when (tone) {
        NoticeTone.Info -> Icons.Outlined.Info
        NoticeTone.Warning -> Icons.Outlined.WarningAmber
        NoticeTone.Suggestion -> Icons.Outlined.AutoAwesome
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(text = title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InlineNoticePreview() {
    SobreioTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            InlineNotice(text = "This account is outside the envelopes.")
            InlineNotice(text = "The envelope may go negative.", tone = NoticeTone.Warning)
            InlineNotice(text = "It was due yesterday.", tone = NoticeTone.Suggestion, title = "Is it Rent?")
        }
    }
}
