package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.AppConfig
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth

import dev.lacruzjs.sobreio.ui.components.clearFocusOnTap
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus
import dev.lacruzjs.sobreio.generated.resources.legal_privacy
import dev.lacruzjs.sobreio.generated.resources.legal_terms
import dev.lacruzjs.sobreio.generated.resources.profile_data_file_name
import dev.lacruzjs.sobreio.generated.resources.profile_download_data
import dev.lacruzjs.sobreio.generated.resources.profile_download_data_note
import dev.lacruzjs.sobreio.generated.resources.profile_legal_accepted_on
import dev.lacruzjs.sobreio.generated.resources.profile_privacy_section
import dev.lacruzjs.sobreio.ui.export.rememberTextFileSaver
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.legal.LegalDocumentTile
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_profile
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.field_display_name
import dev.lacruzjs.sobreio.generated.resources.profile_name_saved
import dev.lacruzjs.sobreio.ui.components.DestructiveButton
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.generated.resources.profile_delete_account
import dev.lacruzjs.sobreio.generated.resources.profile_app_version
import dev.lacruzjs.sobreio.generated.resources.profile_accessibility_section
import dev.lacruzjs.sobreio.generated.resources.profile_color_blind_label
import dev.lacruzjs.sobreio.generated.resources.profile_color_blind_note
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val saveFile = rememberTextFileSaver(mimeType = "application/json") { result ->
        viewModel.onEvent(ProfileEvent.DataFileHandled(result))
    }
    val dataFileName = state.pendingData?.let { stringResource(Res.string.profile_data_file_name, it.day.toString()) }
    // The data is read: open the save dialog once (navigation pattern: a result is state the Route consumes)
    state.pendingData?.let { data ->
        LaunchedEffect(data) { saveFile(dataFileName.orEmpty(), data.content) }
    }
    ProfileScreen(
        state = state,
        appVersion = AppConfig.APP_VERSION,
        languageTag = Locale.current.toLanguageTag(),
        onEvent = { event ->
            when (event) {
                ProfileEvent.BackClicked -> onBack()
                ProfileEvent.DeleteAccountClicked -> onDeleteAccount()
                is ProfileEvent.OpenDocument -> uriHandler.openUri(event.url)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
    /** The installed version (sobreio.version), so a reported problem can be tied to a release. */
    appVersion: String = "",
    languageTag: String = "en",
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.contentMaxWidth(ContentWidth.Form)) {
            ScreenHeader(
                title = stringResource(Res.string.action_profile),
                onBack = { onEvent(ProfileEvent.BackClicked) },
            )
            Column(
                modifier = Modifier
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                FormTextField(
                    value = state.name,
                    onValueChange = { onEvent(ProfileEvent.NameChanged(it)) },
                    label = stringResource(Res.string.field_display_name),
                    capitalization = KeyboardCapitalization.Sentences,
                    errorRes = state.nameError,
                    enabled = !state.isSaving,
                    imeAction = ImeAction.Done,
                    onImeAction = { onEvent(ProfileEvent.SaveClicked) },
                )
                state.user?.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ErrorText(state.saveError, modifier = Modifier.fillMaxWidth())
                if (state.saved) {
                    Text(
                        text = stringResource(Res.string.profile_name_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PrimaryButton(
                    text = stringResource(Res.string.action_save),
                    onClick = { onEvent(ProfileEvent.SaveClicked) },
                    isLoading = state.isSaving,
                    enabled = state.isDirty,
                )
                FormSection(title = stringResource(Res.string.profile_accessibility_section)) {
                    FormTile(
                        label = stringResource(Res.string.profile_color_blind_label),
                        note = stringResource(Res.string.profile_color_blind_note),
                    ) {
                        Switch(
                            checked = state.colorBlindSafe,
                            onCheckedChange = { onEvent(ProfileEvent.ColorBlindSafeChanged(it)) },
                        )
                    }
                }
                FormSection(title = stringResource(Res.string.profile_privacy_section)) {
                    val terms = state.terms
                    val privacy = state.privacy
                    if (terms != null && privacy != null) {
                        LegalDocumentTile(
                            label = stringResource(Res.string.legal_terms),
                            note = acceptedNote(terms, languageTag),
                        ) { onEvent(ProfileEvent.OpenDocument(terms.document.urlFor(languageTag))) }
                        LegalDocumentTile(
                            label = stringResource(Res.string.legal_privacy),
                            note = acceptedNote(privacy, languageTag),
                        ) { onEvent(ProfileEvent.OpenDocument(privacy.document.urlFor(languageTag))) }
                    }
                    FormTile(
                        label = stringResource(Res.string.profile_download_data),
                        note = stringResource(Res.string.profile_download_data_note),
                        onClick = { onEvent(ProfileEvent.DownloadDataClicked) },
                        enabled = !state.isDownloading,
                    ) {
                        if (state.isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                ErrorText(state.downloadError, modifier = Modifier.fillMaxWidth())
                state.downloadMessage?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DestructiveButton(
                    text = stringResource(Res.string.profile_delete_account),
                    onClick = { onEvent(ProfileEvent.DeleteAccountClicked) },
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (appVersion.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.profile_app_version, appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

/** "Accepted on <day>", or nothing while the user has not accepted the version in force. */
@Composable
private fun acceptedNote(status: LegalDocumentStatus, languageTag: String): String? {
    val acceptedAt = status.acceptedAt ?: return null
    if (status.needsAcceptance) return null
    return stringResource(Res.string.profile_legal_accepted_on, formatDate(acceptedAt, languageTag))
}

@PreviewLightDark
@Composable
private fun ProfileScreenPreview() {
    SobreioTheme {
        ProfileScreen(
            state = ProfileUiState(user = User("1", "ana@example.com", "Ana"), name = "Ana"),
            onEvent = {},
            appVersion = "1.0.0",
        )
    }
}
