package dev.lacruzjs.sobreio.ui.legal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.LegalDocument
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_continue
import dev.lacruzjs.sobreio.generated.resources.action_sign_out
import dev.lacruzjs.sobreio.generated.resources.legal_accept_label
import dev.lacruzjs.sobreio.generated.resources.legal_gate_documents
import dev.lacruzjs.sobreio.generated.resources.legal_gate_intro
import dev.lacruzjs.sobreio.generated.resources.legal_gate_sign_out_note
import dev.lacruzjs.sobreio.generated.resources.legal_gate_title
import dev.lacruzjs.sobreio.generated.resources.legal_gate_updated
import dev.lacruzjs.sobreio.generated.resources.legal_privacy
import dev.lacruzjs.sobreio.generated.resources.legal_terms
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Sits between the session and the app (D-57): [content] shows once the user has accepted the documents in force.
 * [checking] is what shows while that is read (the root's splash). Keyed by user, so another account checks afresh.
 */
@Composable
fun LegalGate(
    userId: String,
    checking: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LegalGateViewModel = koinViewModel(key = "legal-gate-$userId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    when (val current = state) {
        LegalGateUiState.Checking -> checking()
        LegalGateUiState.Clear -> content()
        is LegalGateUiState.Required -> LegalAcceptanceScreen(
            state = current,
            languageTag = Locale.current.toLanguageTag(),
            onEvent = { event ->
                when (event) {
                    is LegalGateEvent.OpenDocument -> uriHandler.openUri(event.url)
                    else -> viewModel.onEvent(event)
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
fun LegalAcceptanceScreen(
    state: LegalGateUiState.Required,
    languageTag: String,
    onEvent: (LegalGateEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = stringResource(Res.string.legal_gate_title), modifier = modifier) {
        if (state.isUpdate) {
            InlineNotice(text = stringResource(Res.string.legal_gate_updated), modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                text = stringResource(Res.string.legal_gate_intro),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LegalDocumentLinks(
            title = stringResource(Res.string.legal_gate_documents),
            terms = state.terms,
            privacy = state.privacy,
            languageTag = languageTag,
            onOpen = { onEvent(LegalGateEvent.OpenDocument(it)) },
        )
        FormTile(
            label = stringResource(Res.string.legal_accept_label),
            onClick = { onEvent(LegalGateEvent.AcceptedChanged(!state.accepted)) },
            enabled = !state.isAccepting && !state.isSigningOut,
        ) {
            Switch(
                checked = state.accepted,
                onCheckedChange = { onEvent(LegalGateEvent.AcceptedChanged(it)) },
                enabled = !state.isAccepting && !state.isSigningOut,
            )
        }
        ErrorText(state.error, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_continue),
            onClick = { onEvent(LegalGateEvent.ContinueClicked) },
            isLoading = state.isAccepting,
            enabled = state.canContinue,
        )
        Text(
            text = stringResource(Res.string.legal_gate_sign_out_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(Res.string.action_sign_out),
            onClick = { onEvent(LegalGateEvent.SignOutClicked) },
            isLoading = state.isSigningOut,
            enabled = !state.isAccepting && !state.isSigningOut,
        )
    }
}

/** The two documents as tiles that open them in the browser; shared by the gate and the profile. */
@Composable
fun LegalDocumentLinks(
    title: String,
    terms: LegalDocument,
    privacy: LegalDocument,
    languageTag: String,
    onOpen: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    termsNote: String? = null,
    privacyNote: String? = null,
) {
    FormSection(title = title, modifier = modifier) {
        LegalDocumentTile(stringResource(Res.string.legal_terms), termsNote) { onOpen(terms.urlFor(languageTag)) }
        LegalDocumentTile(stringResource(Res.string.legal_privacy), privacyNote) { onOpen(privacy.urlFor(languageTag)) }
    }
}

/** A tile that opens a legal document in the browser (the gate and the profile). */
@Composable
fun LegalDocumentTile(label: String, note: String?, onClick: () -> Unit) {
    FormTile(label = label, note = note, onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun LegalAcceptanceScreenPreview() {
    val version = LocalDate(2026, 12, 1)
    SobreioTheme {
        LegalAcceptanceScreen(
            state = LegalGateUiState.Required(
                terms = LegalDocument(LegalDocumentKind.Terms, version, "https://example.test/terminos/", "https://example.test/en/terms/"),
                privacy = LegalDocument(LegalDocumentKind.Privacy, version, "https://example.test/privacidad/", "https://example.test/en/privacy/"),
                isUpdate = false,
                accepted = true,
            ),
            languageTag = "es-ES",
            onEvent = {},
        )
    }
}
