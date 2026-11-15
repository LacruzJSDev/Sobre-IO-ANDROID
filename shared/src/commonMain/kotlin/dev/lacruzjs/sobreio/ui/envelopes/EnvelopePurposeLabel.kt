package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_bills
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_free
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_monthly
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_note_bills
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_note_free
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_note_monthly
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_note_savings
import dev.lacruzjs.sobreio.generated.resources.envelope_purpose_savings
import org.jetbrains.compose.resources.StringResource

/** The name of an envelope type: the choice of the form, the section of the list and the line of the detail. */
internal fun EnvelopePurpose.labelRes(): StringResource = when (this) {
    EnvelopePurpose.Monthly -> Res.string.envelope_purpose_monthly
    EnvelopePurpose.Bills -> Res.string.envelope_purpose_bills
    EnvelopePurpose.Savings -> Res.string.envelope_purpose_savings
    EnvelopePurpose.Free -> Res.string.envelope_purpose_free
}

/** What the type does, shown at the end of the envelope form. */
internal fun EnvelopePurpose.noteRes(): StringResource = when (this) {
    EnvelopePurpose.Monthly -> Res.string.envelope_purpose_note_monthly
    EnvelopePurpose.Bills -> Res.string.envelope_purpose_note_bills
    EnvelopePurpose.Savings -> Res.string.envelope_purpose_note_savings
    EnvelopePurpose.Free -> Res.string.envelope_purpose_note_free
}
