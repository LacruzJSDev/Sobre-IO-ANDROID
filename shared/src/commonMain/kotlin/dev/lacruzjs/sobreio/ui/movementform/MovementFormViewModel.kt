package dev.lacruzjs.sobreio.ui.movementform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.LastAccountStore
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.MovementFormRepository
import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.MAX_TAG_NAME_LENGTH
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.GroupTransferDraft
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.TransactionDraft
import dev.lacruzjs.sobreio.domain.model.TransferDraft
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_duplicate_name
import dev.lacruzjs.sobreio.generated.resources.validation_account_required
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.generated.resources.validation_tag_name_too_long
import dev.lacruzjs.sobreio.domain.normalizeTagName
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.theme.TagColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * The create and edit form of a movement. [movementId] null creates; otherwise [source] (a
 * [MovementSource] name) says whether it is a transaction or an account transfer. [fixedAccountId] preselects the account
 * when the form is opened from an account's list; [prefill] opens a new movement with the kind, accounts and amount already
 * chosen (the Group tab's transfer suggestions).
 *
 * Creating calls the database functions (cover included); editing is a plain update validated by the database guards.
 * Tags are written after the movement in their own step: if that step fails the movement stays, the error is shown and a
 * new "Save" only retries the tags.
 */
class MovementFormViewModel(
    private val groupId: String,
    private val movementId: String?,
    private val source: String?,
    private val fixedAccountId: String?,
    private val prefill: MovementPrefill?,
    private val form: MovementFormRepository,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val tags: TagRepository,
    private val detail: AccountDetailRepository,
    private val auth: AuthRepository,
    private val lastAccount: LastAccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MovementFormUiState(isEditing = movementId != null))
    val state: StateFlow<MovementFormUiState> = _state.asStateFlow()

    /** Tags the movement had when loaded: the sync inserts and deletes the difference. */
    private var storedTagIds: Set<String> = emptySet()

    /** Set once the movement exists in the database, so a retry after a tag failure does not create it again. */
    private var createdId: String? = null

    init {
        load()
    }

    fun onEvent(event: MovementFormEvent) {
        when (event) {
            is MovementFormEvent.KindSelected -> selectKind(event.kind)
            is MovementFormEvent.AmountChanged -> edit { it.copy(amount = event.value, amountError = null) }
            is MovementFormEvent.AccountSelected -> selectAccount(event.id)
            is MovementFormEvent.EnvelopeSelected ->
                edit { it.copy(envelopeId = event.id, envelopeError = null, coverEnvelopeId = null) }
            is MovementFormEvent.CoverSelected -> edit { it.copy(coverEnvelopeId = event.id) }
            is MovementFormEvent.ToAccountSelected -> edit { it.copy(toAccountId = event.id, toAccountError = null) }
            is MovementFormEvent.CrossGroupChanged -> edit {
                it.copy(crossGroup = event.value && !it.isEditing, toAccountError = null, destGroupError = null, destAccountError = null)
            }
            is MovementFormEvent.DestGroupSelected -> selectDestGroup(event.id)
            is MovementFormEvent.DestAccountSelected -> edit { it.copy(destAccountId = event.id, destAccountError = null) }
            is MovementFormEvent.DateChanged -> edit { it.copy(date = event.date) }
            is MovementFormEvent.NotesChanged -> edit { it.copy(notes = event.value) }
            is MovementFormEvent.TagToggled -> edit {
                it.copy(selectedTagIds = if (event.id in it.selectedTagIds) it.selectedTagIds - event.id else it.selectedTagIds + event.id)
            }
            is MovementFormEvent.TagQueryChanged -> _state.update { it.copy(tagQuery = event.value, tagError = null, tagFailure = null) }
            MovementFormEvent.TagCreateClicked -> createTag()
            MovementFormEvent.SaveClicked -> save()
            MovementFormEvent.Retry -> load()
            MovementFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            MovementFormEvent.CloseClicked -> Unit
        }
    }

    private fun edit(change: (MovementFormUiState) -> MovementFormUiState) =
        _state.update { change(it).copy(submitError = null) }

    // Choices ------------------------------------------------------------------------------------------------------

    /** Only while creating (the kind is locked on edit). Expense starts with no envelope, income with "Unassigned". */
    private fun selectKind(kind: EntryKind) {
        val current = _state.value
        if (current.isEditing || current.kind == kind) return
        edit {
            it.copy(
                kind = kind,
                envelopeId = if (kind == EntryKind.Income) it.unassigned?.id else null,
                coverEnvelopeId = null,
                envelopeError = null,
                // A transfer cannot go to its own source
                toAccountId = it.toAccountId.takeUnless { to -> to == it.accountId },
                toAccountError = null,
                amountError = null,
            )
        }
    }

    private fun selectAccount(id: String?) {
        edit {
            it.copy(
                accountId = id,
                accountError = null,
                toAccountId = it.toAccountId.takeUnless { to -> to == id },
                toAccountError = null,
            )
        }
    }

    private fun selectDestGroup(id: String?) {
        if (_state.value.destGroupId == id) return
        _state.update {
            it.copy(
                destGroupId = id,
                destGroupError = null,
                destAccounts = emptyList(),
                destAccountId = null,
                submitError = null,
            )
        }
        if (id != null) loadDestination(id)
    }

    private fun loadDestination(destGroupId: String) {
        _state.update { it.copy(isLoadingDestination = true) }
        viewModelScope.launch {
            val result = accounts.getAccounts(destGroupId)
            // The user may have picked another group while this one was loading
            if (_state.value.destGroupId != destGroupId) return@launch
            _state.update {
                it.copy(
                    isLoadingDestination = false,
                    destAccounts = result.getOrDefault(emptyList()).filter { a -> !a.isArchived }.sortedBy { a -> a.name.lowercase() },
                    submitError = result.exceptionOrNull()?.asAppError() ?: it.submitError,
                )
            }
        }
    }

    // Tags ---------------------------------------------------------------------------------------------------------

    /**
     * The picker's "Create" row: a tag named like the search text, with the palette's first colour, selected at once.
     * The picker only offers it when no live tag has that name (ignoring case and accents); if the database still says
     * the name is taken, the error shows under the search field.
     */
    private fun createTag() {
        val current = _state.value
        if (current.isCreatingTag) return
        val name = normalizeTagName(current.tagQuery)
        val error = when {
            name.isEmpty() -> Res.string.validation_required
            name.length > MAX_TAG_NAME_LENGTH -> Res.string.validation_tag_name_too_long
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(tagError = error) }
            return
        }
        _state.update { it.copy(isCreatingTag = true, tagError = null, tagFailure = null) }
        viewModelScope.launch {
            tags.createTag(groupId, name, TagColor.Default.hex)
                .onSuccess { tag ->
                    _state.update {
                        it.copy(
                            isCreatingTag = false,
                            tagQuery = "",
                            tags = (it.tags + tag).sortedBy { t -> t.name.lowercase() },
                            selectedTagIds = it.selectedTagIds + tag.id,
                        )
                    }
                }
                .onFailure { e ->
                    val failure = e.asAppError()
                    _state.update {
                        if (failure == AppError.DuplicateName) {
                            it.copy(isCreatingTag = false, tagError = Res.string.error_duplicate_name)
                        } else {
                            it.copy(isCreatingTag = false, tagFailure = failure)
                        }
                    }
                }
        }
    }

    // Load ---------------------------------------------------------------------------------------------------------

    private fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val groupResult = groups.getGroup(groupId)
            val todayResult = detail.getToday(groupId)
            val accountsResult = accounts.getAccounts(groupId)
            val envelopesResult = envelopes.getEnvelopes(groupId)
            val failure = groupResult.exceptionOrNull()
                ?: todayResult.exceptionOrNull()
                ?: accountsResult.exceptionOrNull()
                ?: envelopesResult.exceptionOrNull()
            if (failure != null) return@launch loadFailed(failure.asAppError())
            val group = groupResult.getOrThrow()
            val today = todayResult.getOrThrow()
            val allAccounts = accountsResult.getOrThrow()
            val liveAccounts = allAccounts.filter { !it.isArchived }.sortedBy { it.name.lowercase() }
            val allEnvelopes = envelopesResult.getOrThrow()
            val liveEnvelopes = allEnvelopes.filter { !it.isArchived }
            // Tags and other groups only decorate the form: without them there is just nothing to pick
            val tagList = tags.getTags(groupId).getOrDefault(emptyList())
            val candidates = groups.getGroups().getOrDefault(emptyList())
                .filter { it.id != groupId && it.currencyCode == group.currencyCode && !it.isArchived && !it.isPendingDeletion }
                .sortedBy { it.name.lowercase() }

            val base = MovementFormUiState(
                isEditing = movementId != null,
                isLoading = false,
                currencyCode = group.currencyCode,
                today = today,
                date = today,
                accounts = liveAccounts,
                envelopes = liveEnvelopes,
                groups = candidates,
                tags = tagList,
            )
            if (movementId == null) {
                val userId = auth.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
                val preselected = listOfNotNull(fixedAccountId, lastAccount.get(userId, groupId))
                    .firstOrNull { id -> liveAccounts.any { it.id == id } }
                _state.update { applyPrefill(base.copy(accountId = preselected)) }
                return@launch
            }
            when (source) {
                MovementSource.Transaction.name -> loadTransaction(base, allAccounts, allEnvelopes)
                MovementSource.AccountTransfer.name -> loadTransfer(base, allAccounts)
                else -> loadFailed(AppError.NotFound)
            }
        }
    }

    /** Only accounts that are live choices are taken; the date stays the group's today. */
    private fun applyPrefill(base: MovementFormUiState): MovementFormUiState {
        val p = prefill ?: return base
        val live = base.accounts.map { it.id }.toSet()
        val from = p.fromAccountId?.takeIf { it in live }
        val to = p.toAccountId?.takeIf { it in live && it != from }
        return base.copy(
            kind = p.kind,
            accountId = from ?: base.accountId.takeIf { p.fromAccountId == null },
            toAccountId = if (p.kind == EntryKind.Transfer) to else null,
            envelopeId = if (p.kind == EntryKind.Income) base.unassigned?.id else null,
            amount = p.amountCents?.toInputText().orEmpty(),
        )
    }

    private suspend fun loadTransaction(
        base: MovementFormUiState,
        allAccounts: List<Account>,
        allEnvelopes: List<Envelope>,
    ) {
        val id = movementId ?: return
        val stored = form.getTransaction(id).getOrElse { return loadFailed(it.asAppError()) }
        val tagIds = form.getTagIds(id).getOrElse { return loadFailed(it.asAppError()) }
        val account = allAccounts.firstOrNull { it.id == stored.accountId } ?: return loadFailed(AppError.NotFound)
        // Changing the account to the other side of the envelope boundary would need the hidden envelope: not offered
        val choices = (base.accounts.filter { it.inEnvelopes == account.inEnvelopes } + account)
            .distinctBy { it.id }.sortedBy { it.name.lowercase() }
        // An archived envelope of the stored row stays a choice; the hidden "Outside envelopes" one is not in the list
        val envelope = allEnvelopes.firstOrNull { it.id == stored.envelopeId }
        val envelopeList = (base.envelopes + listOfNotNull(envelope)).distinctBy { it.id }
        storedTagIds = tagIds
        _state.update {
            base.copy(
                kind = stored.kind,
                amount = stored.amountCents.toInputText(),
                storedAmountCents = stored.amountCents,
                accounts = choices,
                accountId = stored.accountId,
                envelopes = envelopeList,
                envelopeId = envelope?.id.takeIf { account.inEnvelopes },
                date = stored.date,
                notes = stored.notes.orEmpty(),
                selectedTagIds = tagIds,
            )
        }
    }

    private suspend fun loadTransfer(base: MovementFormUiState, allAccounts: List<Account>) {
        val id = movementId ?: return
        val stored = form.getTransfer(id).getOrElse { return loadFailed(it.asAppError()) }
        val from = allAccounts.firstOrNull { it.id == stored.fromAccountId } ?: return loadFailed(AppError.NotFound)
        val to = allAccounts.firstOrNull { it.id == stored.toAccountId } ?: return loadFailed(AppError.NotFound)
        val choices = (base.accounts + from + to).distinctBy { it.id }.sortedBy { it.name.lowercase() }
        _state.update {
            base.copy(
                kind = EntryKind.Transfer,
                amount = stored.amountCents.toInputText(),
                storedAmountCents = stored.amountCents,
                accounts = choices,
                accountId = stored.fromAccountId,
                toAccountId = stored.toAccountId,
                date = stored.date,
                notes = stored.notes.orEmpty(),
            )
        }
    }

    private fun loadFailed(error: AppError) = _state.update { it.copy(isLoading = false, loadError = error) }

    // Save ---------------------------------------------------------------------------------------------------------

    private fun save() {
        val s = _state.value
        if (s.isSubmitting || s.isLoading || s.loadError != null) return
        // The movement exists already: only the tag step is left
        createdId?.let { id ->
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            viewModelScope.launch { syncTags(id).onSuccess { finish() }.onFailure { fail(it) } }
            return
        }
        val cents = parseMoneyInput(s.amount)
        val amountError = when {
            cents == null -> Res.string.validation_amount_invalid
            cents <= 0 -> Res.string.validation_amount_positive
            else -> null
        }
        val accountError = if (s.accountId == null) Res.string.validation_account_required else null
        val envelopeError = if (s.showsEnvelope && s.kind == EntryKind.Expense && s.envelopeId == null) {
            Res.string.validation_envelope_required
        } else null
        val toAccountError = when {
            !s.isTransfer || s.crossGroup -> null
            s.toAccountId == null -> Res.string.validation_destination_account
            s.toAccountId == s.accountId -> Res.string.validation_accounts_differ
            else -> null
        }
        val destGroupError = if (s.isTransfer && s.crossGroup && s.destGroupId == null) Res.string.validation_destination_group else null
        val destAccountError = if (s.isTransfer && s.crossGroup && s.destGroupId != null && s.destAccountId == null) {
            Res.string.validation_destination_account
        } else null
        val date = s.date
        if (amountError != null || accountError != null || envelopeError != null || toAccountError != null ||
            destGroupError != null || destAccountError != null || cents == null || date == null
        ) {
            _state.update {
                it.copy(
                    amountError = amountError,
                    accountError = accountError,
                    envelopeError = envelopeError,
                    toAccountError = toAccountError,
                    destGroupError = destGroupError,
                    destAccountError = destAccountError,
                )
            }
            return
        }
        val accountId = s.accountId ?: return
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch { write(s, accountId, cents, date) }
    }

    private suspend fun write(s: MovementFormUiState, accountId: String, cents: Long, date: LocalDate) {
        val notes = s.notes.trim().ifEmpty { null }
        if (s.isTransfer) {
            val result = when {
                movementId != null ->
                    form.updateTransfer(movementId, TransferDraft(accountId, s.toAccountId.orEmpty(), cents, date, notes)).map { movementId }
                s.crossGroup ->
                    form.createGroupTransfer(GroupTransferDraft(accountId, s.destAccountId.orEmpty(), cents, date, notes))
                else -> form.createTransfer(TransferDraft(accountId, s.toAccountId.orEmpty(), cents, date, notes))
            }
            result.onSuccess { if (movementId == null) rememberAccount(accountId); finish() }.onFailure { fail(it) }
            return
        }
        // An account outside the envelopes has no envelope to send: the database uses the hidden one
        val envelopeId = if (s.accountInEnvelopes) s.envelopeId ?: s.unassigned?.id else null
        val cover = if (s.showsCoverNote) {
            s.coverEnvelopeId
                ?.let { id -> s.envelopes.firstOrNull { it.id == id } }
                ?.let { listOf(CoverItem(it.id, it.balanceCents)) }
                .orEmpty()
        } else {
            emptyList()
        }
        val draft = TransactionDraft(accountId, s.kind, cents, envelopeId, date, notes, cover)
        val saved = if (movementId != null) form.updateTransaction(movementId, draft).map { movementId } else form.createTransaction(draft)
        val id = saved.getOrElse { return fail(it) }
        if (movementId == null) {
            createdId = id
            rememberAccount(accountId)
        }
        syncTags(id).onSuccess { finish() }.onFailure { fail(it) }
    }

    private suspend fun syncTags(transactionId: String): Result<Unit> {
        val selected = _state.value.selectedTagIds
        val added = selected - storedTagIds
        val removed = storedTagIds - selected
        if (added.isEmpty() && removed.isEmpty()) return Result.success(Unit)
        return form.syncTags(groupId, transactionId, added, removed).onSuccess { storedTagIds = selected }
    }

    /** The account used last, to preselect it next time; a failure to read the session only skips the memory. */
    private suspend fun rememberAccount(accountId: String) {
        val userId = (auth.authState.first { it is AuthState.SignedIn } as? AuthState.SignedIn)?.user?.id ?: return
        lastAccount.set(userId, groupId, accountId)
    }

    private fun finish() = _state.update { it.copy(isSubmitting = false, saved = true) }

    private fun fail(e: Throwable) = _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) }

    /** Cents as the text the user would type: `12.50` style with a plain point. */
    private fun Long.toInputText(): String = when {
        this <= 0 -> ""
        this % 100 == 0L -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }
}
