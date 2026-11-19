package dev.lacruzjs.sobreio.ui.components

import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import org.jetbrains.compose.resources.stringResource

/**
 * The name an envelope is shown with. The database stores the system envelope "Unassigned" with a Spanish name, so the
 * app shows its own translated one; every other envelope keeps its name.
 */
@Composable
fun Envelope.displayName(): String =
    if (kind == EnvelopeKind.System) stringResource(Res.string.envelopes_unassigned) else name
