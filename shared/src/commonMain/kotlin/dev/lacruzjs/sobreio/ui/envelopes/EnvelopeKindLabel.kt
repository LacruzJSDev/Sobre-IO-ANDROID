package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_saving
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_spending
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import org.jetbrains.compose.resources.StringResource

/** Never the name the database stores for the system envelope: that one is in Spanish. */
internal fun EnvelopeKind.labelRes(): StringResource = when (this) {
    EnvelopeKind.System -> Res.string.envelopes_unassigned
    EnvelopeKind.Spending -> Res.string.envelopes_section_spending
    EnvelopeKind.Saving -> Res.string.envelopes_section_saving
}
