package dev.lacruzjs.sobreio.ui.envelopetransfers

import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_commitment
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_commitment_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_cover
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_manual
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_release
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_release_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_target
import org.jetbrains.compose.resources.stringResource

/** What made a move between envelopes, as a row label: "Assignment", "Monthly target", "Reserve for X"... */
@Composable
fun EnvelopeTransfer.originLabel(): String = when (origin) {
    TransferOrigin.Manual -> stringResource(Res.string.transfer_origin_manual)
    TransferOrigin.Target -> stringResource(Res.string.transfer_origin_target)
    TransferOrigin.Commitment -> commitmentName
        ?.let { stringResource(Res.string.transfer_origin_commitment, it) }
        ?: stringResource(Res.string.transfer_origin_commitment_plain)
    TransferOrigin.Cover -> stringResource(Res.string.transfer_origin_cover)
    TransferOrigin.Release -> commitmentName
        ?.let { stringResource(Res.string.transfer_origin_release, it) }
        ?: stringResource(Res.string.transfer_origin_release_plain)
}
