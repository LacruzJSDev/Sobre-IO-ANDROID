package dev.lacruzjs.sobreio.ui.components

import dev.lacruzjs.sobreio.ui.theme.onHeroContainer
import dev.lacruzjs.sobreio.ui.theme.HeroLabelAlpha
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_what_is_this
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The small ⓘ next to a figure whose meaning is not obvious (really free, free to distribute…): a tap opens a short sheet
 * that says what the figure is and where it comes from. [paragraphs] are string resources, so every text is bilingual.
 */
@Composable
fun InfoButton(
    title: String,
    paragraphs: List<StringResource>,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(Res.string.action_what_is_this, title)
    LabelTooltip(label, modifier = modifier) {
        IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
    if (open) {
        FlatBottomSheet(title = title, onDismiss = { open = false }) {
            paragraphs.forEach {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** A hero card label with its ⓘ right after it, in the card's own colours. */
@Composable
fun HeroLabelWithInfo(
    text: String,
    infoTitle: String,
    paragraphs: List<StringResource>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HeroLabel(text)
        InfoButton(
            title = infoTitle,
            paragraphs = paragraphs,
            tint = MaterialTheme.colorScheme.onHeroContainer.copy(alpha = HeroLabelAlpha),
        )
    }
}
