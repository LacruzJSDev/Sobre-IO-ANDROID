package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_duplicate_name
import dev.lacruzjs.sobreio.generated.resources.validation_funded_needs_bills
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * The create and edit form of a commitment of one account ([accountId]); [commitmentId] null creates.
 *
 * Creating sends everything to `create_commitment`, or to `create_linked_commitment` for a contribution to another
 * group. Editing renames when the name changed and then calls `change_commitment` (a new version from the chosen date)
 * or, for a contribution, `change_linked_commitment` (the amount only). Each step that succeeded is remembered, so a
 * retry after a failure in a later step does not repeat it.
 */
class CommitmentFormViewModel(
    private val groupId: String,
    private val accountId: String,
    private val commitmentId: String?,
    private val commitments: CommitmentRepository,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommitmentFormUiState(isEditing = commitmentId != null, isLoading = true))
    val state: StateFlow<CommitmentFormUiState> = _state.asStateFlow()

    /** What the database has; compared with the form to know which steps are left. */
    private var storedName = ""
    private var storedAmountCents = 0L
    private var link: CommitmentLink? = null

    /** Any rule field (not the name) was changed since the load: only then is a new version worth creating. */
    private var rulesTouched = false

    init {
        load()
    }

    fun onEvent(event: CommitmentFormEvent) {
        when (event) {
            is CommitmentFormEvent.NameChanged -> edit(touchRules = false) { it.copy(name = event.value, nameError = null) }
            is CommitmentFormEvent.KindSelected -> edit { it.copy(kind = event.kind, autoRegister = event.kind == CommitmentKind.GroupTransferOut, funded = false, envelopeError = null, toAccountError = null) }
            is CommitmentFormEvent.AmountChanged -> edit { it.copy(amount = event.value, amountError = null) }
            is CommitmentFormEvent.AccountSelected -> selectAccount(event.id)
            is CommitmentFormEvent.ToAccountSelected -> edit { it.copy(toAccountId = event.id, toAccountError = null) }
            is CommitmentFormEvent.EnvelopeSelected -> edit { it.copy(envelopeId = event.id, envelopeError = null) }
            is CommitmentFormEvent.DestGroupSelected -> selectDestGroup(event.id)
            is CommitmentFormEvent.DestAccountSelected -> edit { it.copy(destAccountId = event.id, destAccountError = null) }
            is CommitmentFormEvent.DestEnvelopeSelected -> edit { it.copy(destEnvelopeId = event.id) }
            is CommitmentFormEvent.FirstDueDateChanged -> edit { it.copy(firstDueDate = event.date, endDateError = null) }
            is CommitmentFormEvent.RecurringChanged -> edit { it.copy(isRecurring = event.value, endDateError = null) }
            is CommitmentFormEvent.EveryChanged -> edit { it.copy(everyN = event.value.coerceIn(MIN_EVERY_N, MAX_EVERY_N)) }
            is CommitmentFormEvent.UnitSelected -> edit { it.copy(unit = event.unit) }
            is CommitmentFormEvent.EndDateChanged -> edit { it.copy(endDate = event.date, endDateError = null) }
            is CommitmentFormEvent.AutoRegisterChanged -> edit { it.copy(autoRegister = event.value) }
            is CommitmentFormEvent.FundedChanged -> edit { it.withFunded(event.value) }
            is CommitmentFormEvent.NotesChanged -> edit { it.copy(notes = event.value) }
            is CommitmentFormEvent.EffectiveFromChanged -> edit(touchRules = false) { it.copy(effectiveFrom = event.date) }
            CommitmentFormEvent.SaveClicked -> save()
            CommitmentFormEvent.Retry -> load()
            CommitmentFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            CommitmentFormEvent.CloseClicked -> Unit
        }
    }

    /** Only while editing a commitment that is not a contribution; an account outside the envelopes has no envelope and no funding. */
    private fun selectAccount(id: String) {
        val current = _state.value
        val chosen = current.accounts.firstOrNull { it.id == id }
        if (!current.canChangeAccount || chosen == null || chosen.id == current.accountId) return
        edit { it.withAccount(chosen) }
    }

    private fun edit(touchRules: Boolean = true, change: (CommitmentFormUiState) -> CommitmentFormUiState) {
        if (touchRules) rulesTouched = true
        _state.update { change(it).copy(submitError = null) }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val groupResult = groups.getGroup(groupId)
            val todayResult = calendar.getToday(groupId)
            val accountsResult = accounts.getAccounts(groupId)
            val envelopesResult = envelopes.getEnvelopes(groupId)
            val failure = groupResult.exceptionOrNull()
                ?: todayResult.exceptionOrNull()
                ?: accountsResult.exceptionOrNull()
                ?: envelopesResult.exceptionOrNull()
            if (failure != null) return@launch loadFailed(failure.asAppError())
            val group = groupResult.getOrThrow()
            val today = todayResult.getOrThrow()
            if (accountsResult.getOrThrow().none { it.id == accountId && !it.isArchived }) return@launch loadFailed(AppError.NotFound)
            val liveAccounts = accountsResult.getOrThrow().filter { !it.isArchived }.sortedBy { it.name.lowercase() }
            val live = envelopesResult.getOrThrow().filter { !it.isArchived && it.kind != EnvelopeKind.System }.sortedBy { it.name.lowercase() }
            // Other groups are only a convenience for contributions: without them that kind simply has no destination
            val candidates = groups.getGroups().getOrDefault(emptyList())
                .filter { it.canEdit && it.id != groupId && it.currencyCode == group.currencyCode }
                .sortedBy { it.name.lowercase() }

            val base = CommitmentFormUiState(
                isEditing = commitmentId != null,
                currencyCode = group.currencyCode,
                accountId = accountId,
                storedAccountId = accountId,
                accounts = liveAccounts,
                envelopes = live,
                groups = candidates,
                today = today,
                firstDueDate = today,
                effectiveFrom = today,
            )
            if (commitmentId == null) {
                _state.update { base }
                return@launch
            }
            val commitment = commitments.getCommitment(commitmentId).getOrElse { return@launch loadFailed(it.asAppError()) }
            // The version of this account: the one in force, or a future one that moves the commitment here
            val version = commitment.versionFor(accountId, today) ?: commitment.currentVersion(today)
            if (version == null || version.kind == CommitmentKind.GroupTransferIn) return@launch loadFailed(AppError.NotFound)
            // The version may belong to another (live) account of the group
            if (liveAccounts.none { it.id == version.accountId }) return@launch loadFailed(AppError.NotFound)
            var edited = base.copy(accountId = version.accountId, storedAccountId = version.accountId)
                .fromVersion(commitment, version, live)
                .let { if (version.effectiveFrom > today) it.copy(effectiveFrom = version.effectiveFrom) else it }
            if (version.kind == CommitmentKind.GroupTransferOut) {
                val found = commitments.getLinks(listOf(commitment.id)).getOrElse { return@launch loadFailed(it.asAppError()) }
                    .firstOrNull { it.outCommitmentId == commitment.id }
                    ?: return@launch loadFailed(AppError.BusinessRule("FZ016"))
                link = found
                // The other half tells where the money goes; only to show it, so a failure leaves those tiles empty
                val other = commitments.getCommitment(found.inCommitmentId).getOrNull()?.currentVersion(today)
                edited = edited.copy(
                    isLinkedEdit = true,
                    destGroupId = found.inGroupId,
                    destAccountId = other?.accountId,
                    destEnvelopeId = other?.envelopeId,
                    destGroupName = groups.getGroup(found.inGroupId).getOrNull()?.name,
                )
            }
            storedName = commitment.name
            storedAmountCents = version.amountCents
            rulesTouched = false
            _state.update { edited }
            if (edited.isLinkedEdit) edited.destGroupId?.let { loadDestination(it) }
        }
    }

    private fun loadFailed(error: AppError) = _state.update { it.copy(isLoading = false, loadError = error) }

    private fun selectDestGroup(id: String) {
        if (_state.value.destGroupId == id) return
        rulesTouched = true
        _state.update {
            it.copy(
                destGroupId = id,
                destGroupName = it.groups.firstOrNull { g -> g.id == id }?.name,
                destGroupError = null,
                destAccounts = emptyList(),
                destAccountId = null,
                destEnvelopes = emptyList(),
                destEnvelopeId = null,
                submitError = null,
            )
        }
        loadDestination(id)
    }

    /** The accounts and envelopes of the destination group, to choose from (or to show their names while editing). */
    private fun loadDestination(destGroupId: String) {
        _state.update { it.copy(isLoadingDestination = true) }
        viewModelScope.launch {
            val accountList = accounts.getAccounts(destGroupId)
            val envelopeList = envelopes.getEnvelopes(destGroupId)
            // The user may have picked another group while this one was loading
            if (_state.value.destGroupId != destGroupId) return@launch
            val failure = accountList.exceptionOrNull() ?: envelopeList.exceptionOrNull()
            _state.update {
                it.copy(
                    isLoadingDestination = false,
                    destAccounts = accountList.getOrDefault(emptyList()).filter { a -> !a.isArchived }.sortedBy { a -> a.name.lowercase() },
                    destEnvelopes = envelopeList.getOrDefault(emptyList())
                        .filter { e -> !e.isArchived && e.kind != EnvelopeKind.System }
                        .sortedBy { e -> e.name.lowercase() },
                    submitError = failure?.asAppError() ?: it.submitError,
                )
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading || current.loadError != null) return
        val name = current.name.trim()
        val cents = parseMoneyInput(current.amount)
        val errors = validateCommitmentForm(current, name, cents, rulesTouched)
        val firstDueDate = current.firstDueDate
        if (!errors.isValid || cents == null || firstDueDate == null || (current.isEditing && current.effectiveFrom == null)) {
            _state.update { it.withErrors(errors) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            if (!current.isEditing) create(current, name, cents, firstDueDate) else update(current, name, cents, firstDueDate)
        }
    }

    private suspend fun create(s: CommitmentFormUiState, name: String, cents: Long, firstDueDate: LocalDate) {
        val result = if (s.isContribution) {
            commitments.createLinkedCommitment(linkedCommitmentDraftOf(s, name, cents, firstDueDate))
        } else {
            commitments.createCommitment(commitmentDraftOf(s, name, cents, firstDueDate))
        }
        result.onSuccess { finish() }.onFailure { fail(it) }
    }

    private suspend fun update(s: CommitmentFormUiState, name: String, cents: Long, firstDueDate: LocalDate) {
        val id = commitmentId ?: return
        if (name != storedName) {
            val renamed = commitments.renameCommitment(id, name)
            if (renamed.isFailure) return fail(renamed.exceptionOrNull() ?: AppError.Unknown())
            // Done in the database: a retry of the next step must not repeat it
            storedName = name
        }
        val linkId = link?.id
        val result = when {
            linkId != null -> if (cents != storedAmountCents) {
                commitments.changeLinkedCommitment(linkId, cents, s.effectiveFrom).onSuccess { storedAmountCents = cents }
            } else {
                Result.success(Unit)
            }
            rulesTouched -> commitments.changeCommitment(id, commitmentChangeOf(s, cents, firstDueDate))
            else -> Result.success(Unit)
        }
        result.onSuccess { finish() }.onFailure { fail(it) }
    }

    private fun finish() = _state.update { it.copy(isSubmitting = false, saved = true) }

    private fun fail(e: Throwable) {
        val error = e.asAppError()
        _state.update {
            if (error == AppError.DuplicateName) {
                it.copy(isSubmitting = false, nameError = Res.string.error_duplicate_name)
            } else if (error is AppError.BusinessRule && error.code == "FZ021") {
                // The envelope is not a Bills one (its type changed since the list was read)
                it.copy(isSubmitting = false, envelopeError = Res.string.validation_funded_needs_bills)
            } else {
                it.copy(isSubmitting = false, submitError = error)
            }
        }
    }

}
